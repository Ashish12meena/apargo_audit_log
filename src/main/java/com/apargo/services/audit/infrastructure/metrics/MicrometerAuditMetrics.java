package com.apargo.services.audit.infrastructure.metrics;

import java.time.Duration;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.port.out.AuditMetrics;
import com.apargo.services.audit.common.constant.MetricNames;
import com.apargo.services.audit.common.enums.DeadLetterReason;
import com.apargo.services.audit.common.enums.LogStream;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/** Micrometer implementation of {@link AuditMetrics}. Tag values are kept low-cardinality. */
@Component
public class MicrometerAuditMetrics implements AuditMetrics {

    private static final Pattern INDEX = Pattern.compile("\\[\\d+]");
    private static final int MAX_TAG_LENGTH = 80;

    private final MeterRegistry registry;

    public MicrometerAuditMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void batchProcessed(LogStream stream, int received, int stored, int duplicates, int deadLettered,
                               Duration took) {
        String tag = stream.tag();
        records(tag, MetricNames.OUTCOME_STORED).increment(stored);
        records(tag, MetricNames.OUTCOME_DUPLICATE).increment(duplicates);
        records(tag, MetricNames.OUTCOME_DEAD_LETTERED).increment(deadLettered);
        DistributionSummary.builder(MetricNames.INGEST_BATCH_SIZE).tag(MetricNames.TAG_STREAM, tag)
                .register(registry).record(received);
        Timer.builder(MetricNames.INGEST_BATCH_DURATION).tag(MetricNames.TAG_STREAM, tag)
                .register(registry).record(took);
    }

    @Override
    public void deadLettered(LogStream stream, DeadLetterReason reason) {
        Counter.builder(MetricNames.INGEST_DEAD_LETTERS)
                .tag(MetricNames.TAG_STREAM, stream.tag())
                .tag(MetricNames.TAG_REASON, reason.name())
                .register(registry).increment();
    }

    @Override
    public void contractViolation(LogStream stream, String sourceService, String rule) {
        Counter.builder(MetricNames.INGEST_CONTRACT_VIOLATIONS)
                .tag(MetricNames.TAG_STREAM, stream.tag())
                .tag(MetricNames.TAG_SOURCE, tagValue(sourceService))
                .tag(MetricNames.TAG_RULE, tagValue(INDEX.matcher(rule).replaceAll("[]")))
                .register(registry).increment();
    }

    @Override
    public void unknownField(String type, String field) {
        Counter.builder(MetricNames.INGEST_UNKNOWN_FIELDS)
                .tag(MetricNames.TAG_TYPE, tagValue(type))
                .tag(MetricNames.TAG_FIELD, tagValue(field))
                .register(registry).increment();
    }

    @Override
    public void archived(LogStream stream, long exported, long deleted) {
        archive(stream, MetricNames.ACTION_EXPORTED).increment(exported);
        archive(stream, MetricNames.ACTION_DELETED).increment(deleted);
    }

    @Override
    public void archiveRun(String outcome) {
        Counter.builder(MetricNames.ARCHIVE_RUNS).tag(MetricNames.TAG_OUTCOME, tagValue(outcome))
                .register(registry).increment();
    }

    private Counter records(String stream, String outcome) {
        return Counter.builder(MetricNames.INGEST_RECORDS)
                .tag(MetricNames.TAG_STREAM, stream)
                .tag(MetricNames.TAG_OUTCOME, outcome)
                .register(registry);
    }

    private Counter archive(LogStream stream, String action) {
        return Counter.builder(MetricNames.ARCHIVE_RECORDS)
                .tag(MetricNames.TAG_STREAM, stream.tag())
                .tag(MetricNames.TAG_ACTION, action)
                .register(registry);
    }

    private static String tagValue(String value) {
        if (value == null || value.isBlank()) {
            return MetricNames.UNKNOWN_TAG_VALUE;
        }
        return value.length() <= MAX_TAG_LENGTH ? value : value.substring(0, MAX_TAG_LENGTH);
    }
}
