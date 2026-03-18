package com.apargo.service.auditlog.dto.request.ingest.messaging.conversation;

public interface BaseConversationStatusIngestRequest {
    String getEventId();
    Long getConversationId();
    Long getOrgId();
    Long getProjectId();
    Long getWabaAccountId();
    Long getContactId();
    com.apargo.service.auditlog.enums.AuditActorType getActorType();
    Long getActorId();
    String getFromStatus();
    String getToStatus();
    java.time.Instant getOccurredAt();
}