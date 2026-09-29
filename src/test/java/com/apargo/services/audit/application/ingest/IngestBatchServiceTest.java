package com.apargo.services.audit.application.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.apargo.platform.contract.event.EventTopics;
import com.apargo.services.audit.TestEvents;
import com.apargo.services.audit.application.port.out.AuditMetrics;
import com.apargo.services.audit.application.port.out.DeadLetterPublisher;
import com.apargo.services.audit.application.port.out.LogWriter;
import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.apargo.services.audit.common.enums.DeadLetterReason;
import com.apargo.services.audit.common.enums.LogStream;
import com.apargo.services.audit.domain.mapping.AccessLogFactory;
import com.apargo.services.audit.domain.mapping.AuditLogFactory;
import com.apargo.services.audit.domain.model.AccessLog;
import com.apargo.services.audit.domain.model.LogRecord;
import com.apargo.services.audit.domain.policy.RetentionPolicy;
import com.apargo.services.audit.domain.validation.AccessEventRules;
import com.apargo.services.audit.domain.validation.AuditEventRules;

class IngestBatchServiceTest {

    private static final String AUDIT_TOPIC = "platform.audit.events";
    private static final String ACCESS_TOPIC = "platform.access.events";

    private final RecordingWriter writer = new RecordingWriter();
    private final RecordingDeadLetters deadLetters = new RecordingDeadLetters();
    private final List<String> unknownFields = new ArrayList<>();
    private final IngestBatchService service = newService(1024 * 64);

    private IngestBatchService newService(int maxBytes) {
        RetentionPolicy retention = new RetentionPolicy(Duration.ofDays(30), Duration.ofDays(30));
        return new IngestBatchService(new EventDecoder((type, field) -> unknownFields.add(type + "." + field)),
                TestEvents.POLICY, new AuditEventRules(TestEvents.POLICY), new AccessEventRules(TestEvents.POLICY),
                new AuditLogFactory(retention), new AccessLogFactory(retention), writer, deadLetters,
                new NoopMetrics(), Clock.fixed(TestEvents.NOW, ZoneOffset.UTC), maxBytes);
    }

    @Test
    void storesValidEvents() {
        BatchResult result = service.ingest(LogStream.AUDIT, List.of(
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json(TestEvents.audit())),
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json(TestEvents.audit()))));

        assertThat(result).isEqualTo(new BatchResult(2, 2, 0, 0));
        assertThat(writer.written).hasSize(2);
        assertThat(deadLetters.published).isEmpty();
    }

    @Test
    void deadLettersPoisonAndStoresTheRest() {
        BatchResult result = service.ingest(LogStream.AUDIT, List.of(
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json("{not json")),
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json(TestEvents.auditBuilder().orgId(null).build())),
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json(TestEvents.audit()))));

        assertThat(result).isEqualTo(new BatchResult(3, 1, 0, 2));
        assertThat(deadLetters.reasons()).containsExactly(DeadLetterReason.DESERIALIZATION, DeadLetterReason.VALIDATION);
    }

    @Test
    void rejectsOversizedRecordsBeforeParsing() {
        IngestBatchService small = newService(1024);
        byte[] large = TestEvents.json(TestEvents.auditBuilder().metadata("blob", "x".repeat(2000)).build());

        small.ingest(LogStream.AUDIT, List.of(TestEvents.inbound(AUDIT_TOPIC, large)));

        assertThat(deadLetters.reasons()).containsExactly(DeadLetterReason.OVERSIZED);
        assertThat(writer.written).isEmpty();
    }

    @Test
    void rejectsAnUnsupportedSchemaVersionHeader() {
        var headers = Map.of(EventTopics.HEADER_SCHEMA_VERSION, "2".getBytes(StandardCharsets.UTF_8));

        service.ingest(LogStream.AUDIT, List.of(
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json(TestEvents.audit()), headers)));

        assertThat(deadLetters.reasons()).containsExactly(DeadLetterReason.UNSUPPORTED_VERSION);
    }

    @Test
    void deadLettersRecordsTheStoreRefused() {
        var refused = TestEvents.audit();
        writer.outcome = records -> new LogWriter.WriteOutcome(records.size() - 1, 0,
                List.of(new LogWriter.RejectedRecord(refused.eventId(), 121, "Document failed validation")));

        BatchResult result = service.ingest(LogStream.AUDIT, List.of(
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json(refused)),
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json(TestEvents.audit()))));

        assertThat(result.deadLettered()).isEqualTo(1);
        assertThat(deadLetters.reasons()).containsExactly(DeadLetterReason.DB_REJECTED);
    }

    @Test
    void countsDuplicatesAsHandled() {
        writer.outcome = records -> new LogWriter.WriteOutcome(0, records.size(), List.of());

        BatchResult result = service.ingest(LogStream.AUDIT, List.of(
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json(TestEvents.audit()))));

        assertThat(result).isEqualTo(new BatchResult(1, 0, 1, 0));
    }

    @Test
    void propagatesTransientFailuresWithoutDeadLettering() {
        writer.outcome = records -> {
            throw new TransientFailureException("mongo down", null);
        };

        assertThatThrownBy(() -> service.ingest(LogStream.AUDIT, List.of(
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json("{not json")),
                TestEvents.inbound(AUDIT_TOPIC, TestEvents.json(TestEvents.audit())))))
                .isInstanceOf(TransientFailureException.class);
        assertThat(deadLetters.published).isEmpty();
    }

    @Test
    void keepsSchemaDocAccessFieldsAndReportsUnknownOnes() {
        String json = """
                {"eventId":"%s","schemaVersion":1,"sourceService":"auth-service","environment":"development",
                 "eventType":"LOGIN_FAILED","status":"FAILURE","orgId":1001,
                 "actor":{"type":"USER","id":"501","email":"ra***@acme.com"},
                 "authMethod":"PASSWORD","failureReason":"INVALID_PASSWORD","country":"IN",
                 "traceId":"%s","occurredAt":"2026-09-25T08:40:20Z","brandNewField":"x"}
                """.formatted(com.apargo.platform.contract.event.EventIds.newEventId(), TestEvents.TRACE_ID);

        service.ingest(LogStream.ACCESS, List.of(TestEvents.inbound(ACCESS_TOPIC, TestEvents.json(json))));

        AccessLog stored = (AccessLog) writer.written.get(0);
        assertThat(stored.failureCode()).isEqualTo("INVALID_PASSWORD");
        assertThat(stored.authMethod()).isEqualTo("PASSWORD");
        assertThat(stored.actor().email()).isEqualTo("ra***@acme.com");
        assertThat(unknownFields).containsExactly("AccessEventMessage.brandNewField");
    }

    private static final class RecordingWriter implements LogWriter {
        final List<LogRecord> written = new ArrayList<>();
        Function<List<? extends LogRecord>, WriteOutcome> outcome =
                records -> new WriteOutcome(records.size(), 0, List.of());

        @Override
        public WriteOutcome write(LogStream stream, List<? extends LogRecord> records) {
            WriteOutcome result = outcome.apply(records);
            written.addAll(records);
            return result;
        }
    }

    private static final class RecordingDeadLetters implements DeadLetterPublisher {
        final List<DeadLetter> published = new ArrayList<>();

        @Override
        public void publish(LogStream stream, List<DeadLetter> letters) {
            published.addAll(letters);
        }

        List<DeadLetterReason> reasons() {
            return published.stream().map(DeadLetter::reason).toList();
        }
    }

    private static final class NoopMetrics implements AuditMetrics {
        @Override
        public void batchProcessed(LogStream s, int r, int st, int d, int dl, Duration t) {
        }

        @Override
        public void deadLettered(LogStream stream, DeadLetterReason reason) {
        }

        @Override
        public void contractViolation(LogStream stream, String sourceService, String rule) {
        }

        @Override
        public void unknownField(String type, String field) {
        }

        @Override
        public void archived(LogStream stream, long exported, long deleted) {
        }

        @Override
        public void archiveRun(String outcome) {
        }
    }
}
