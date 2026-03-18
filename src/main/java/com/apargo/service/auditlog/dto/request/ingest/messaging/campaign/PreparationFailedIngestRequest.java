package com.apargo.service.auditlog.dto.request.ingest.messaging.campaign;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseCampaignAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PreparationFailedIngestRequest
        extends BaseCampaignAuditIngestRequest {

    @NotNull
    private String toStatus;             // FAILED

    @NotNull
    private String failureStage;         // "template_fetch", "contact_fetch"

    @NotNull
    private String failureReason;
}