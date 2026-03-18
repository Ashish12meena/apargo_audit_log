package com.apargo.service.auditlog.dto.request.ingest.messaging.campaign;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseCampaignAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CampaignCreatedIngestRequest
        extends BaseCampaignAuditIngestRequest {

    @NotNull
    private String toStatus;             // PREPARING or SCHEDULED

    @NotNull
    private Long templateId;

    @NotNull
    private Integer totalPhoneNumbers;   // after deduplication

    @NotNull
    private Integer mediaCount;
}