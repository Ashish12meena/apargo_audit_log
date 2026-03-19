package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateSyncAuditIngestRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateSyncFailedIngestRequest
        extends BaseTemplateSyncAuditIngestRequest {

    @NotNull
    private String failureReason;
}