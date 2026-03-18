package com.apargo.service.auditlog.repository.messaging;

import com.apargo.service.auditlog.model.messaging.MessagingCampaignAuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagingCampaignAuditRepository
        extends MongoRepository<MessagingCampaignAuditEvent, String> {

    boolean existsByEventId(String eventId);

    List<MessagingCampaignAuditEvent> findByCampaignIdOrderByOccurredAtAsc(Long campaignId);

    List<MessagingCampaignAuditEvent> findByOrgIdAndProjectIdOrderByOccurredAtDesc(
            Long orgId, Long projectId);
}