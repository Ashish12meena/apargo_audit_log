package com.apargo.service.auditlog.dto.request;

import com.apargo.service.auditlog.enums.AuditActorType;
import com.apargo.service.auditlog.enums.AuditEventStatus;
import com.apargo.service.auditlog.enums.AuditEventType;
import com.apargo.service.auditlog.enums.Module;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * One shape for every incoming audit event, regardless of module or entry
 * point (REST ingest controller or Kafka consumer). Previously every module
 * had its own hierarchy of *IngestRequest classes (TemplateCreatedIngestRequest,
 * CampaignCreatedIngestRequest, ...); since everything now lands in a single
 * collection, one request DTO covers all of them and module-specific data
 * goes in {@code metadata}.
 */
@Data
@Builder
public class AuditIngestRequest {

    /**
     * Idempotency key. Optional — if the producing service doesn't supply
     * one (common for some Kafka producers), IdempotencyUtil derives a
     * deterministic one from the event's natural identity.
     */
    private String eventId;

    @NotNull
    private Module module;

    @NotNull
    private AuditEventType eventType;

    @NotNull
    private AuditEventStatus eventStatus;

    @NotNull
    private Long orgId;

    private Long projectId; // nullable

    @NotNull
    private AuditActorType actorType;

    private Long actorId; // nullable for SYSTEM/SCHEDULER/WORKER

    @NotNull
    private String entityType;

    private String entityId; // nullable for org/project-scoped events with no single entity

    private String requestId; // nullable
    private String traceId; // nullable
    private String ipAddress; // nullable
    private String userAgent; // nullable

    private Map<String, Object> oldValue; // nullable
    private Map<String, Object> newValue; // nullable

    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    private String errorMessage; // nullable, expected when eventStatus == FAILURE

    @NotNull
    private Instant occurredAt;
}
