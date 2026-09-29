package com.apargo.services.audit.api.request;

import java.time.Instant;

import com.apargo.services.audit.api.context.RequestContext;
import com.apargo.services.audit.application.query.AuditLogFilter;

/**
 * Query parameters of the audit log list. Tenant scope is never a parameter: it comes from
 * {@code X-Org-Id} / {@code X-Project-Id}. Validation happens in the query service so every
 * route applies the same rules.
 */
public record AuditLogSearchParams(
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
        Instant to,
        String cursor,
        Integer size) {

    public AuditLogFilter toFilter(RequestContext context) {
        return toFilter(context, traceId);
    }

    public AuditLogFilter toFilter(RequestContext context, String traceIdOverride) {
        return new AuditLogFilter(context.orgId(), context.projectId(), blankToNull(module), blankToNull(eventType),
                blankToNull(status), blankToNull(actorId), blankToNull(entityType), blankToNull(entityId),
                blankToNull(sourceService), blankToNull(channel), blankToNull(errorCode),
                blankToNull(traceIdOverride), from, to);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
