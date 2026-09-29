package com.apargo.services.audit.application.port.out;

import java.time.Duration;

import com.apargo.services.audit.common.enums.DeadLetterReason;
import com.apargo.services.audit.common.enums.LogStream;

/** Operational metrics emitted by the use cases. */
public interface AuditMetrics {

    void batchProcessed(LogStream stream, int received, int stored, int duplicates, int deadLettered, Duration took);

    void deadLettered(LogStream stream, DeadLetterReason reason);

    void contractViolation(LogStream stream, String sourceService, String rule);

    void unknownField(String type, String field);

    void archived(LogStream stream, long exported, long deleted);

    void archiveRun(String outcome);
}
