package com.apargo.services.audit.application.port.out;

import java.util.List;

import com.apargo.services.audit.common.enums.LogStream;
import com.apargo.services.audit.domain.model.LogRecord;

/** Stores log records. Implementations must be idempotent on {@code eventId}. */
public interface LogWriter {

    /**
     * Inserts all records. A record whose id already exists counts as a duplicate (success).
     * A record the store refuses for its own content is returned as rejected.
     *
     * @throws TransientFailureException when the store could not complete the write; retrying the
     *                                   same records is always safe
     */
    WriteOutcome write(LogStream stream, List<? extends LogRecord> records);

    record WriteOutcome(int stored, int duplicates, List<RejectedRecord> rejected) {

        public WriteOutcome {
            rejected = List.copyOf(rejected);
        }

        public static WriteOutcome empty() {
            return new WriteOutcome(0, 0, List.of());
        }
    }

    record RejectedRecord(String eventId, int errorCode, String message) {
    }
}
