package com.apargo.service.auditlog.dto.request.ingest.messaging.dispatch;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseDispatchAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class KafkaPublishFailedIngestRequest
        extends BaseDispatchAuditIngestRequest {

    @NotNull
    private Integer batchSize;           // entire batch failed

    @NotNull
    private String kafkaTopic;

    @NotNull
    private String errorMessage;
}