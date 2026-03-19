package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateStatusTransitionIngestRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateRejectedIngestRequest
        extends BaseTemplateStatusTransitionIngestRequest {

    /**
     * Rejection reason at this moment in time.
     * Captured here because it can be overwritten later in the template table.
     */
    @NotNull
    private String rejectionReason;
}