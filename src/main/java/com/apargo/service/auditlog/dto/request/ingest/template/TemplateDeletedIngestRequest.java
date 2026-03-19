package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateAuditIngestRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateDeletedIngestRequest
        extends BaseTemplateAuditIngestRequest {

    /**
     * True if deleteFromMeta=true was passed and
     * Facebook deletion was attempted.
     */
    @NotNull
    private Boolean deletedFromMeta;
}