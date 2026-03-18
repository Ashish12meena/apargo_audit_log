package com.apargo.service.auditlog.service.ingestion.messaging;

import com.apargo.service.auditlog.dto.request.ingest.messaging.dispatch.*;
import com.apargo.service.auditlog.enums.DispatchAuditEventType;
import com.apargo.service.auditlog.model.messaging.MessagingDispatchAuditEvent;
import com.apargo.service.auditlog.repository.messaging.MessagingDispatchAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchAuditIngestionService {

    private final MessagingDispatchAuditRepository repository;

    public void handleBatchPublished(KafkaBatchPublishedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingDispatchAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(DispatchAuditEventType.KAFKA_BATCH_PUBLISHED)
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .batchSize(req.getBatchSize())
                .successCount(req.getSuccessCount())
                .kafkaTopic(req.getKafkaTopic())
                .workerName(req.getWorkerName())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handlePublishFailed(KafkaPublishFailedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingDispatchAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(DispatchAuditEventType.KAFKA_PUBLISH_FAILED)
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .batchSize(req.getBatchSize())
                .failureCount(req.getBatchSize())
                .kafkaTopic(req.getKafkaTopic())
                .workerName(req.getWorkerName())
                .errorMessage(req.getErrorMessage())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handlePartiallyFailed(BatchPartiallyFailedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingDispatchAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(DispatchAuditEventType.BATCH_PARTIALLY_FAILED)
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .batchSize(req.getBatchSize())
                .successCount(req.getSuccessCount())
                .failureCount(req.getFailureCount())
                .workerName(req.getWorkerName())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleStaleLockRecovered(StaleLockRecoveredIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingDispatchAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(DispatchAuditEventType.STALE_LOCK_RECOVERED)
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .recoveredCount(req.getRecoveredCount())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleMaxAttempts(RecipientMaxAttemptsIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingDispatchAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(DispatchAuditEventType.RECIPIENT_MAX_ATTEMPTS_EXHAUSTED)
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .failureCount(req.getFailureCount())
                .workerName(req.getWorkerName())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    private boolean isDuplicate(String eventId) {
        if (repository.existsByEventId(eventId)) {
            log.warn("Duplicate dispatch audit event ignored: eventId={}", eventId);
            return true;
        }
        return false;
    }
}