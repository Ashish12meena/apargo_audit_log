package com.apargo.services.audit;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import com.apargo.platform.contract.audit.AuditActorDto;
import com.apargo.platform.contract.audit.AuditChannel;
import com.apargo.platform.contract.audit.AuditEntityDto;
import com.apargo.platform.contract.audit.AuditEventDto;
import com.apargo.platform.contract.audit.AuditEventStatus;
import com.apargo.platform.contract.event.EventIds;
import com.apargo.services.audit.application.ingest.InboundEvent;
import com.apargo.services.audit.domain.event.AccessEventMessage;
import com.apargo.services.audit.domain.model.AccessActor;
import com.apargo.services.audit.domain.validation.AcceptancePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Valid contract events and their JSON, for tests. */
public final class TestEvents {

    public static final String ENVIRONMENT = "development";
    public static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    public static final Instant NOW = Instant.parse("2026-09-25T08:45:21Z");
    public static final AcceptancePolicy POLICY =
            new AcceptancePolicy(ENVIRONMENT, java.util.Set.of(1), java.time.Duration.ofHours(24));

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private TestEvents() {
    }

    public static AuditEventDto.Builder auditBuilder() {
        return AuditEventDto.builder()
                .eventId(EventIds.newEventId())
                .schemaVersion(1)
                .sourceService("template-service")
                .environment(ENVIRONMENT)
                .module("TEMPLATE")
                .eventType("TEMPLATE_UPDATED")
                .status(AuditEventStatus.SUCCESS)
                .orgId(1001L)
                .projectId(2001L)
                .actor(AuditActorDto.user("501"))
                .entity(new AuditEntityDto("TEMPLATE", "tpl-10001", "order_confirmation"))
                .change("status", "DRAFT", "APPROVED")
                .metadata("wabaId", "waba-001")
                .channel(AuditChannel.WEB)
                .requestId("req-7a92")
                .traceId(TRACE_ID)
                .occurredAt(Instant.parse("2026-09-25T08:45:20Z"));
    }

    public static AuditEventDto audit() {
        return auditBuilder().build();
    }

    public static AccessEventMessage access(String status, String failureCode) {
        return new AccessEventMessage(EventIds.newEventId(), 1, "auth-service", ENVIRONMENT, "LOGIN_FAILED", status,
                1001L, new AccessActor("USER", "501", "Rahul", null, null), null, failureCode, null,
                "192.168.1.20", "Mozilla/5.0", null, null, "req-login-1", TRACE_ID,
                Instant.parse("2026-09-25T08:40:20Z"));
    }

    public static byte[] json(Object value) {
        try {
            return JSON.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] json(String raw) {
        return raw.getBytes(StandardCharsets.UTF_8);
    }

    public static InboundEvent inbound(String topic, byte[] value) {
        return inbound(topic, value, Map.of());
    }

    public static InboundEvent inbound(String topic, byte[] value, Map<String, byte[]> headers) {
        return new InboundEvent(topic, 0, 42L, "key", value, headers);
    }
}
