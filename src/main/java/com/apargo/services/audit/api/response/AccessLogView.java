package com.apargo.services.audit.api.response;

import java.time.Instant;
import java.util.Map;

import com.apargo.services.audit.domain.model.AccessActor;
import com.fasterxml.jackson.annotation.JsonInclude;

/** One access log in API responses. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccessLogView(
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
        Instant recordedAt) {
}
