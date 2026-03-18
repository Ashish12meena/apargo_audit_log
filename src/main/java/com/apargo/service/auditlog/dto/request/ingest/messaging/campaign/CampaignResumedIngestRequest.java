package com.apargo.service.auditlog.dto.request.ingest.messaging.campaign;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseCampaignAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CampaignResumedIngestRequest
        extends BaseCampaignAuditIngestRequest {

    @NotNull
    private String fromStatus;           // PAUSED

    @NotNull
    private String toStatus;             // RUNNING
}