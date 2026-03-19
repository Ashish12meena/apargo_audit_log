package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.enums.AuditActorType;
import com.apargo.service.auditlog.enums.TemplateAuditEventType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class TemplateSyncAuditIngestRequest {

    @NotNull
    private String eventId;

    @NotNull
    private TemplateAuditEventType eventType;

    @NotNull
    private Long orgId;

    @NotNull
    private Long projectId;

    @NotNull
    private String wabaId;

    private Integer insertedCount;
    private Integer updatedCount;
    private Integer deletedCount;
    private Integer skippedCount;
    private Long durationMs;
    private String failureReason;

    @NotNull
    private AuditActorType actorType;

    private Long actorId;

    @NotNull
    private Instant occurredAt;
}