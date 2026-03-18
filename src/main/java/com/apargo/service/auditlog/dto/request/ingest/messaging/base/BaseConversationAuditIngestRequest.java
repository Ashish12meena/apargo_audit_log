package com.apargo.service.auditlog.dto.request.ingest.messaging.base;

import com.apargo.service.auditlog.enums.AuditActorType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseConversationAuditIngestRequest
        extends BaseMessagingAuditIngestRequest {

    @NotNull
    private Long conversationId;

    @NotNull
    private Long contactId;

    @NotNull
    private AuditActorType actorType;

    /**
     * Null for SYSTEM and WORKER events.
     */
    private Long actorId;
}