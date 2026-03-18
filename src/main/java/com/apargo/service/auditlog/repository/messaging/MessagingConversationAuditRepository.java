package com.apargo.service.auditlog.repository.messaging;

import com.apargo.service.auditlog.model.messaging.MessagingConversationAuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagingConversationAuditRepository
        extends MongoRepository<MessagingConversationAuditEvent, String> {

    boolean existsByEventId(String eventId);

    List<MessagingConversationAuditEvent> findByConversationIdOrderByOccurredAtAsc(
            Long conversationId);
}