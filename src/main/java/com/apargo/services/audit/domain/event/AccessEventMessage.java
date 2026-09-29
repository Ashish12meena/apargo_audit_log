package com.apargo.services.audit.domain.event;

import java.time.Instant;
import java.util.Map;

import com.apargo.services.audit.domain.model.AccessActor;
import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * An access event as read from Kafka: every {@code AccessEventDto} field, plus the schema-doc
 * fields producers may start sending later. Reading into the contract DTO would silently drop
 * those extras. {@code status} is a string so values such as {@code DENIED} are accepted.
 */
public record AccessEventMessage(
        String eventId,
        Integer schemaVersion,
        String sourceService,
        String environment,
        String eventType,
        String status,
        Long orgId,
        AccessActor actor,
        String authMethod,
        @JsonAlias("failureReason") String failureCode,
        String resource,
        String ip,
        String userAgent,
        String country,
        Map<String, Object> metadata,
        String requestId,
        String traceId,
        Instant occurredAt) {
}
