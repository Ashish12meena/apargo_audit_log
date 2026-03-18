package com.apargo.service.auditlog.dto.request.ingest.messaging.conversation;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseConversationAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationCreatedIngestRequest
        extends BaseConversationAuditIngestRequest {

    @NotNull
    private String toStatus;             // OPEN
}