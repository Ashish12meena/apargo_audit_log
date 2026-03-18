package com.apargo.service.auditlog.dto.request.ingest.messaging.conversation;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseConversationAuditIngestRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConversationReassignedIngestRequest
        extends BaseConversationAuditIngestRequest {

    @NotNull
    private String assignedType;

    @NotNull
    private Long assignedId;             // assigned TO

    @NotNull
    private Long previousAssignedId;     // assigned FROM — mandatory for reassignment
}