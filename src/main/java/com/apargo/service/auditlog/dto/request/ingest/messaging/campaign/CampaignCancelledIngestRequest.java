package com.apargo.service.auditlog.dto.request.ingest.messaging.campaign;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseCampaignAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CampaignCancelledIngestRequest
        extends BaseCampaignAuditIngestRequest {

    @NotNull
    private String fromStatus;

    @NotNull
    private String toStatus;             // CANCELLED

    @NotNull
    private String cancelledAtStage;     // "PREPARING", "RUNNING", "SCHEDULED"
}