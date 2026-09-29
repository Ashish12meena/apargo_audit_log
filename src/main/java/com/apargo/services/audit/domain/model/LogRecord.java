package com.apargo.services.audit.domain.model;

import java.time.Instant;

import com.apargo.services.audit.common.enums.LogStream;

/** A stored, append-only log entry. */
public sealed interface LogRecord permits AuditLog, AccessLog {

    String eventId();

    LogStream stream();

    Instant occurredAt();
}
