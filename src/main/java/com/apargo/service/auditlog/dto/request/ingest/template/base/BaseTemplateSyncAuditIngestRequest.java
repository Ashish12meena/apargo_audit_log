package com.apargo.service.auditlog.dto.request.ingest.template.base;


import com.apargo.service.auditlog.enums.AuditActorType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public abstract class BaseTemplateSyncAuditIngestRequest {

    @NotNull
    private String eventId;

    @NotNull
    private Long orgId;

    @NotNull
    private Long projectId;

    @NotNull
    private String wabaId;

    @NotNull
    private AuditActorType actorType;

    /**
     * Null for WORKER events.
     */
    private Long actorId;

    @NotNull
    private Instant occurredAt;
}