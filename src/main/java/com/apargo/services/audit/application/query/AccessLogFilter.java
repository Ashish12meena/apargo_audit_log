package com.apargo.services.audit.application.query;

import java.time.Instant;

/** Filters of an access log search. {@code orgId} {@code null} is allowed only internally. */
public record AccessLogFilter(
        Long orgId,
        String eventType,
        String status,
        String actorId,
        String traceId,
        Instant from,
        Instant to) {

    public AccessLogFilter withRange(TimeRange range) {
        return new AccessLogFilter(orgId, eventType, status, actorId, traceId, range.from(), range.to());
    }
}
