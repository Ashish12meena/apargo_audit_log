package com.apargo.service.auditlog.dto.request.ingest.template;


import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateStatusTransitionIngestRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateUpdatedIngestRequest
        extends BaseTemplateStatusTransitionIngestRequest {
    // fromStatus = DRAFT, toStatus = DRAFT
    // nothing extra needed — template service has the new component/variable state
}