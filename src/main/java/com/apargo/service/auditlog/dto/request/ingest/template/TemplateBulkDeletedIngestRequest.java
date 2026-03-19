package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateAuditIngestRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateBulkDeletedIngestRequest
        extends BaseTemplateAuditIngestRequest {

    /**
     * Number of templates soft-deleted in this bulk operation.
     */
    @NotNull
    private Integer deletedCount;
}