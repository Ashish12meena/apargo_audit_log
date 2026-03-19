package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateSyncAuditIngestRequest;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateSyncStartedIngestRequest
        extends BaseTemplateSyncAuditIngestRequest {
    // no extra fields — just marks that sync was accepted and background job fired
}