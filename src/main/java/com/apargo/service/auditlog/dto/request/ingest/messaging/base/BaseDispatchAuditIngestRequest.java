package com.apargo.service.auditlog.dto.request.ingest.messaging.base;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseDispatchAuditIngestRequest
        extends BaseMessagingAuditIngestRequest {

    @NotNull
    private Long campaignId;

    /**
     * Which worker pod processed this batch.
     * e.g. "dispatch-a1b2c3d4"
     * Null for scheduler-triggered events like STALE_LOCK_RECOVERED.
     */
    private String workerName;
}