package com.apargo.services.audit.domain.model;

import java.time.Instant;
import java.util.Map;

import com.apargo.services.audit.common.enums.LogStream;

/**
 * One {@code access_logs} document: every {@code AccessEventDto} field plus the optional
 * schema-doc fields, and {@code recordedAt} / {@code archiveAt} set by this service.
 */
public record AccessLog(
        String eventId,
        int schemaVersion,
        String sourceService,
        String eventType,
        String status,
        Long orgId,
        AccessActor actor,
        String authMethod,
        String failureCode,
        String resource,
        String ip,
        String userAgent,
        String country,
        Map<String, Object> metadata,
        String requestId,
        String traceId,
        Instant occurredAt,
        Instant recordedAt,
        Instant archiveAt) implements LogRecord {

    @Override
    public LogStream stream() {
        return LogStream.ACCESS;
    }
}
