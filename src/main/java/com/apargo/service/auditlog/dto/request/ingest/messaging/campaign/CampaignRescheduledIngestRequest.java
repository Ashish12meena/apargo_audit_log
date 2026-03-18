package com.apargo.service.auditlog.dto.request.ingest.messaging.campaign;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseCampaignAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
public class CampaignRescheduledIngestRequest
        extends BaseCampaignAuditIngestRequest {

    @NotNull
    private String fromStatus;           // SCHEDULED

    @NotNull
    private String toStatus;             // SCHEDULED

    @NotNull
    private Instant previousScheduledAt;

    @NotNull
    private Instant newScheduledAt;
}