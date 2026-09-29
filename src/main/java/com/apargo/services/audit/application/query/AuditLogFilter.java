package com.apargo.services.audit.application.query;

import java.time.Instant;

/**
 * Filters of an audit log search. {@code orgId} is the tenant scope from {@code X-Org-Id};
 * {@code null} is allowed only for internal trace lookups.
 */
public record AuditLogFilter(
        Long orgId,
        Long projectId,
        String module,
        String eventType,
        String status,
        String actorId,
        String entityType,
        String entityId,
        String sourceService,
        String channel,
        String errorCode,
        String traceId,
        Instant from,
        Instant to) {

    public AuditLogFilter withRange(TimeRange range) {
        return new AuditLogFilter(orgId, projectId, module, eventType, status, actorId, entityType, entityId,
                sourceService, channel, errorCode, traceId, range.from(), range.to());
    }
}
