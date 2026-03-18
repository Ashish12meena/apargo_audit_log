package com.apargo.service.auditlog.dto.request.ingest.messaging.base;

import com.apargo.service.auditlog.enums.AuditActorType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseCampaignAuditIngestRequest
        extends BaseMessagingAuditIngestRequest {

    @NotNull
    private Long campaignId;

    @NotNull
    private AuditActorType actorType;

    /**
     * Null for SCHEDULER and WORKER events.
     */
    private Long actorId;
}