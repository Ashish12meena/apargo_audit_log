package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateStatusTransitionIngestRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateCreatedIngestRequest
        extends BaseTemplateStatusTransitionIngestRequest {

    /**
     * True  → saved as DRAFT only, not sent to Meta.
     * False → submitted directly to Meta after save.
     */
    @NotNull
    private Boolean isDraft;
}