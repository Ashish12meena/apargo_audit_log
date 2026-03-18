package com.apargo.service.auditlog.repository.messaging;

import com.apargo.service.auditlog.model.messaging.MessagingDispatchAuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagingDispatchAuditRepository
        extends MongoRepository<MessagingDispatchAuditEvent, String> {

    boolean existsByEventId(String eventId);

    List<MessagingDispatchAuditEvent> findByCampaignIdOrderByOccurredAtAsc(Long campaignId);
}