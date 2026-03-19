package com.apargo.service.auditlog.dto.request.ingest.template.base;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseTemplateStatusTransitionIngestRequest
        extends BaseTemplateAuditIngestRequest {

    /**
     * Null for TEMPLATE_CREATED — no prior status exists.
     */
    private String fromStatus;

    @NotNull
    private String toStatus;
}