package com.apargo.service.auditlog.dto.request.ingest.messaging.dispatch;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseDispatchAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KafkaBatchPublishedIngestRequest
        extends BaseDispatchAuditIngestRequest {

    @NotNull
    private Integer batchSize;

    @NotNull
    private Integer successCount;

    @NotNull
    private String kafkaTopic;
}