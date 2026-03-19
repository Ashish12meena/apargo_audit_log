package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateAuditIngestRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateCategoryChangedIngestRequest
        extends BaseTemplateAuditIngestRequest {

    @NotNull
    private String previousCategory;

    @NotNull
    private String newCategory;
}