package com.apargo.service.auditlog.dto.request.ingest.messaging.conversation;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseConversationAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationAssignedIngestRequest
        extends BaseConversationAuditIngestRequest {

    @NotNull
    private String assignedType;         // USER or TEAM

    @NotNull
    private Long assignedId;
}