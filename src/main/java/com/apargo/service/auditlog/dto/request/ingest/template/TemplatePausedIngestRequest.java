package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateStatusTransitionIngestRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplatePausedIngestRequest
        extends BaseTemplateStatusTransitionIngestRequest {

    /**
     * Reason provided by Meta for pausing.
     * Null if Meta did not provide one.
     */
    private String rejectionReason;
}