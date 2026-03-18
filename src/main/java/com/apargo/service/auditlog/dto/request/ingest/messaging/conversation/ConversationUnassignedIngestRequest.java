package com.apargo.service.auditlog.dto.request.ingest.messaging.conversation;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseConversationAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationUnassignedIngestRequest
        extends BaseConversationAuditIngestRequest {

    @NotNull
    private Long previousAssignedId;
}