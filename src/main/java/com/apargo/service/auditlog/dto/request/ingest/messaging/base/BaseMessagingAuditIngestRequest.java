package com.apargo.service.auditlog.dto.request.ingest.messaging.base;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public abstract class BaseMessagingAuditIngestRequest {

    @NotNull
    private String eventId;

    @NotNull
    private Long orgId;

    @NotNull
    private Long projectId;

    @NotNull
    private Long wabaAccountId;

    @NotNull
    private Instant occurredAt;
}