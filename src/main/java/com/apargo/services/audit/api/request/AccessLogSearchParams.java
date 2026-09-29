package com.apargo.services.audit.api.request;

import java.time.Instant;

import com.apargo.services.audit.api.context.RequestContext;
import com.apargo.services.audit.application.query.AccessLogFilter;

/** Query parameters of the access log list. Tenant scope comes from {@code X-Org-Id}. */
public record AccessLogSearchParams(
        String eventType,
        String status,
        String actorId,
        String traceId,
        Instant from,
        Instant to,
        String cursor,
        Integer size) {

    public AccessLogFilter toFilter(RequestContext context) {
        return new AccessLogFilter(context.orgId(), AuditLogSearchParams.blankToNull(eventType),
                AuditLogSearchParams.blankToNull(status), AuditLogSearchParams.blankToNull(actorId),
                AuditLogSearchParams.blankToNull(traceId), from, to);
    }
}
