package com.apargo.service.auditlog.dto.request.ingest.messaging.campaign;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseCampaignAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DispatchStartedIngestRequest
        extends BaseCampaignAuditIngestRequest {

    @NotNull
    private String fromStatus;           // READY

    @NotNull
    private String toStatus;             // RUNNING

    @NotNull
    private Boolean isRecovery;          // false = first run, true = pod restart
}