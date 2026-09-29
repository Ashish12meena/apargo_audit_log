package com.apargo.services.audit.common.enums;

/** Why a record was sent to the dead-letter topic. Only permanent (poison) failures appear here. */
public enum DeadLetterReason {
    /** Value is not valid JSON for the contract, or carries an enum value this service does not know. */
    DESERIALIZATION,
    /** A blocking rule failed (id, org, time, type, format…). */
    VALIDATION,
    /** schemaVersion (header or body) is not one this service supports. */
    UNSUPPORTED_VERSION,
    /** The event was produced for another environment. */
    ENV_MISMATCH,
    /** The record is larger than the configured maximum event size. */
    OVERSIZED,
    /** MongoDB rejected the document (validation or size) for this record only. */
    DB_REJECTED
}
