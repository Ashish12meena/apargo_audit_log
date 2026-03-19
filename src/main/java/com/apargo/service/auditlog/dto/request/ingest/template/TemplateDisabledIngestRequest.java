package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateStatusTransitionIngestRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateDisabledIngestRequest
        extends BaseTemplateStatusTransitionIngestRequest {
    // fromStatus = APPROVED or PAUSED, toStatus = DISABLED
}