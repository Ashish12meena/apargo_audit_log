package com.apargo.services.audit.application.ingest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apargo.platform.contract.audit.AuditEventDto;
import com.apargo.platform.contract.event.EventTopics;
import com.apargo.services.audit.application.port.out.AuditMetrics;
import com.apargo.services.audit.application.port.out.DeadLetterPublisher;
import com.apargo.services.audit.application.port.out.LogWriter;
import com.apargo.services.audit.application.port.out.LogWriter.RejectedRecord;
import com.apargo.services.audit.application.port.out.LogWriter.WriteOutcome;
import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.apargo.services.audit.common.enums.DeadLetterReason;
import com.apargo.services.audit.common.enums.LogStream;
import com.apargo.services.audit.domain.event.AccessEventMessage;
import com.apargo.services.audit.domain.mapping.AccessLogFactory;
import com.apargo.services.audit.domain.mapping.AuditLogFactory;
import com.apargo.services.audit.domain.model.LogRecord;
import com.apargo.services.audit.domain.validation.AcceptancePolicy;
import com.apargo.services.audit.domain.validation.AccessEventRules;
import com.apargo.services.audit.domain.validation.AuditEventRules;
import com.apargo.services.audit.domain.validation.ValidationResult;

/**
 * The only write path. For one polled batch:
 * <ol>
 *   <li>check size and schema-version header, decode, validate, map each record;</li>
 *   <li>write every accepted record in one bulk insert (duplicates count as stored);</li>
 *   <li>dead-letter the poison records and the ones the store refused, and wait for the broker.</li>
 * </ol>
 * It returns only when every record is either stored or dead-lettered, so the caller may commit
 * offsets. Any {@link TransientFailureException} means "retry the same batch"; that is always safe
 * because writes are idempotent on {@code eventId}.
 */
public final class IngestBatchService {

    private static final Logger log = LoggerFactory.getLogger(IngestBatchService.class);

    private final EventDecoder decoder;
    private final AcceptancePolicy policy;
    private final AuditEventRules auditRules;
    private final AccessEventRules accessRules;
    private final AuditLogFactory auditLogFactory;
    private final AccessLogFactory accessLogFactory;
    private final LogWriter writer;
    private final DeadLetterPublisher deadLetterPublisher;
    private final AuditMetrics metrics;
    private final Clock clock;
    private final int maxEventBytes;

    public IngestBatchService(EventDecoder decoder, AcceptancePolicy policy, AuditEventRules auditRules,
                              AccessEventRules accessRules, AuditLogFactory auditLogFactory,
                              AccessLogFactory accessLogFactory, LogWriter writer,
                              DeadLetterPublisher deadLetterPublisher, AuditMetrics metrics, Clock clock,
                              int maxEventBytes) {
        this.decoder = Objects.requireNonNull(decoder);
        this.policy = Objects.requireNonNull(policy);
        this.auditRules = Objects.requireNonNull(auditRules);
        this.accessRules = Objects.requireNonNull(accessRules);
        this.auditLogFactory = Objects.requireNonNull(auditLogFactory);
        this.accessLogFactory = Objects.requireNonNull(accessLogFactory);
        this.writer = Objects.requireNonNull(writer);
        this.deadLetterPublisher = Objects.requireNonNull(deadLetterPublisher);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
        if (maxEventBytes <= 0) {
            throw new IllegalArgumentException("maxEventBytes must be positive");
        }
        this.maxEventBytes = maxEventBytes;
    }

    public BatchResult ingest(LogStream stream, List<InboundEvent> events) {
        if (events.isEmpty()) {
            return new BatchResult(0, 0, 0, 0);
        }
        long started = System.nanoTime();
        Instant recordedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);

        List<LogRecord> accepted = new ArrayList<>(events.size());
        Map<String, InboundEvent> sourceById = new HashMap<>();
        List<DeadLetter> deadLetters = new ArrayList<>();

        for (InboundEvent event : events) {
            Prepared prepared = prepare(stream, event, recordedAt);
            if (prepared.deadLetter() != null) {
                deadLetters.add(prepared.deadLetter());
            } else {
                accepted.add(prepared.record());
                sourceById.putIfAbsent(prepared.record().eventId(), event);
            }
        }

        WriteOutcome outcome = accepted.isEmpty() ? WriteOutcome.empty() : writer.write(stream, accepted);
        for (RejectedRecord rejected : outcome.rejected()) {
            deadLetters.add(new DeadLetter(sourceById.get(rejected.eventId()), DeadLetterReason.DB_REJECTED,
                    "mongo error " + rejected.errorCode() + ": " + rejected.message()));
        }

        if (!deadLetters.isEmpty()) {
            deadLetterPublisher.publish(stream, deadLetters);
            for (DeadLetter deadLetter : deadLetters) {
                metrics.deadLettered(stream, deadLetter.reason());
                log.warn("ingest_dead_lettered stream={} reason={} location={} key={} detail={}",
                        stream.tag(), deadLetter.reason(), deadLetter.source().location(),
                        deadLetter.source().key(), deadLetter.detail());
            }
        }

        BatchResult result = new BatchResult(events.size(), outcome.stored(), outcome.duplicates(), deadLetters.size());
        Duration took = Duration.ofNanos(System.nanoTime() - started);
        metrics.batchProcessed(stream, result.received(), result.stored(), result.duplicates(),
                result.deadLettered(), took);
        log.debug("ingest_batch_done stream={} received={} stored={} duplicates={} deadLettered={} tookMs={}",
                stream.tag(), result.received(), result.stored(), result.duplicates(), result.deadLettered(),
                took.toMillis());
        return result;
    }

    private Prepared prepare(LogStream stream, InboundEvent event, Instant recordedAt) {
        if (event.size() > maxEventBytes) {
            return Prepared.rejected(event, DeadLetterReason.OVERSIZED,
                    "value is " + event.size() + " bytes, limit " + maxEventBytes);
        }
        String headerVersion = event.headerText(EventTopics.HEADER_SCHEMA_VERSION);
        if (headerVersion != null && !policy.supports(parseVersion(headerVersion))) {
            return Prepared.rejected(event, DeadLetterReason.UNSUPPORTED_VERSION,
                    "schemaVersion header '" + headerVersion + "' is not supported");
        }
        return switch (stream) {
            case AUDIT -> prepareAudit(event, recordedAt);
            case ACCESS -> prepareAccess(event, recordedAt);
        };
    }

    private Prepared prepareAudit(InboundEvent event, Instant recordedAt) {
        EventDecoder.Decoded<AuditEventDto> decoded = decoder.decode(event.value(), AuditEventDto.class);
        if (!decoded.isSuccess()) {
            return Prepared.rejected(event, DeadLetterReason.DESERIALIZATION, decoded.error());
        }
        AuditEventDto dto = decoded.value();
        ValidationResult result = auditRules.evaluate(dto, recordedAt);
        if (!result.isAccepted()) {
            return Prepared.rejected(event, result.rejectReason(), String.join("; ", result.problems()));
        }
        report(LogStream.AUDIT, dto.eventId(), dto.sourceService(), result);
        return Prepared.accepted(auditLogFactory.create(dto, recordedAt));
    }

    private Prepared prepareAccess(InboundEvent event, Instant recordedAt) {
        EventDecoder.Decoded<AccessEventMessage> decoded = decoder.decode(event.value(), AccessEventMessage.class);
        if (!decoded.isSuccess()) {
            return Prepared.rejected(event, DeadLetterReason.DESERIALIZATION, decoded.error());
        }
        AccessEventMessage message = decoded.value();
        ValidationResult result = accessRules.evaluate(message, recordedAt);
        if (!result.isAccepted()) {
            return Prepared.rejected(event, result.rejectReason(), String.join("; ", result.problems()));
        }
        report(LogStream.ACCESS, message.eventId(), message.sourceService(), result);
        return Prepared.accepted(accessLogFactory.create(message, recordedAt));
    }

    private void report(LogStream stream, String eventId, String sourceService, ValidationResult result) {
        if (result.reported().isEmpty()) {
            return;
        }
        result.reported().forEach(rule -> metrics.contractViolation(stream, sourceService, rule));
        log.warn("ingest_contract_violation stream={} eventId={} sourceService={} problems={}",
                stream.tag(), eventId, sourceService, result.reported());
    }

    private static Integer parseVersion(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record Prepared(LogRecord record, DeadLetter deadLetter) {

        static Prepared accepted(LogRecord record) {
            return new Prepared(record, null);
        }

        static Prepared rejected(InboundEvent event, DeadLetterReason reason, String detail) {
            return new Prepared(null, new DeadLetter(event, reason, detail));
        }
    }
}
