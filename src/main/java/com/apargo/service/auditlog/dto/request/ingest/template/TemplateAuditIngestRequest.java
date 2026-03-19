package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.enums.AuditActorType;
import com.apargo.service.auditlog.enums.TemplateAuditEventType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class TemplateAuditIngestRequest {

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

    private Long templateId;

    private String fromStatus;
    private String toStatus;

    private Boolean isDraft;
    private Boolean deletedFromMeta;
    private Integer deletedCount;
    private String rejectionReason;
    private String previousCategory;
    private String newCategory;

    @NotNull
    private AuditActorType actorType;

    private Long actorId;

    @NotNull
    private Instant occurredAt;
}