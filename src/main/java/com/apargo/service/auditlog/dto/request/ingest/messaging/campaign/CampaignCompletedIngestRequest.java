package com.apargo.service.auditlog.dto.request.ingest.messaging.campaign;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseCampaignAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CampaignCompletedIngestRequest
        extends BaseCampaignAuditIngestRequest {

    @NotNull
    private String fromStatus;           // RUNNING

    @NotNull
    private String toStatus;             // COMPLETED

    @NotNull
    private Long sentCount;

    @NotNull
    private Long failedCount;

    @NotNull
    private Long skippedCount;
}