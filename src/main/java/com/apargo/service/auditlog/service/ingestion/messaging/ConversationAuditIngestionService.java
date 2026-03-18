package com.apargo.service.auditlog.service.ingestion.messaging;

import com.apargo.service.auditlog.dto.request.ingest.messaging.base.BaseConversationAuditIngestRequest;
import com.apargo.service.auditlog.dto.request.ingest.messaging.conversation.*;
import com.apargo.service.auditlog.enums.ConversationAuditEventType;
import com.apargo.service.auditlog.model.messaging.MessagingConversationAuditEvent;
import com.apargo.service.auditlog.repository.messaging.MessagingConversationAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationAuditIngestionService {

    private final MessagingConversationAuditRepository repository;

    public void handleCreated(ConversationCreatedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingConversationAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(ConversationAuditEventType.CONVERSATION_CREATED)
                .toStatus(req.getToStatus())
                .conversationId(req.getConversationId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .contactId(req.getContactId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleAssigned(ConversationAssignedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingConversationAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(ConversationAuditEventType.CONVERSATION_ASSIGNED)
                .conversationId(req.getConversationId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .contactId(req.getContactId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .assignedType(req.getAssignedType())
                .assignedId(req.getAssignedId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleReassigned(ConversationReassignedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingConversationAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(ConversationAuditEventType.CONVERSATION_REASSIGNED)
                .conversationId(req.getConversationId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .contactId(req.getContactId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .assignedType(req.getAssignedType())
                .assignedId(req.getAssignedId())
                .previousAssignedId(req.getPreviousAssignedId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleUnassigned(ConversationUnassignedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingConversationAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(ConversationAuditEventType.CONVERSATION_UNASSIGNED)
                .conversationId(req.getConversationId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .contactId(req.getContactId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .previousAssignedId(req.getPreviousAssignedId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleClosed(ConversationClosedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;
        save(req, ConversationAuditEventType.CONVERSATION_CLOSED,
                req.getFromStatus(), req.getToStatus());
    }

    public void handleReopened(ConversationReopenedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;
        save(req, ConversationAuditEventType.CONVERSATION_REOPENED,
                req.getFromStatus(), req.getToStatus());
    }

    public void handleArchived(ConversationArchivedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;
        save(req, ConversationAuditEventType.CONVERSATION_ARCHIVED,
                req.getFromStatus(), req.getToStatus());
    }

    // closed, reopened, archived all share the same shape — extracted to avoid repetition
    private void save(BaseConversationAuditIngestRequest req,
                      ConversationAuditEventType type,
                      String fromStatus,
                      String toStatus) {
        repository.save(MessagingConversationAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(type)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .conversationId(req.getConversationId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .contactId(req.getContactId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    private boolean isDuplicate(String eventId) {
        if (repository.existsByEventId(eventId)) {
            log.warn("Duplicate conversation audit event ignored: eventId={}", eventId);
            return true;
        }
        return false;
    }
}