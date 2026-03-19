package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateStatusTransitionIngestRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateSubmittedIngestRequest
        extends BaseTemplateStatusTransitionIngestRequest {
    // fromStatus = DRAFT, toStatus = SUBMITTED
    // nothing extra needed beyond base transition fields
}