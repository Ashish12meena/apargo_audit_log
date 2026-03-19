package com.apargo.service.auditlog.dto.request.ingest.template;

import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateSyncAuditIngestRequest;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateSyncCompletedIngestRequest
        extends BaseTemplateSyncAuditIngestRequest {

    @NotNull
    private Integer insertedCount;

    @NotNull
    private Integer updatedCount;

    @NotNull
    private Integer deletedCount;

    @NotNull
    private Integer skippedCount;

    /**
     * Total time the background sync job took in milliseconds.
     */
    @NotNull
    private Long durationMs;
}