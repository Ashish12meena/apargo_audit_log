package com.apargo.service.auditlog.service.ingestion.messaging;

import com.apargo.service.auditlog.dto.request.ingest.messaging.campaign.*;
import com.apargo.service.auditlog.enums.CampaignAuditEventType;
import com.apargo.service.auditlog.model.messaging.MessagingCampaignAuditEvent;
import com.apargo.service.auditlog.repository.messaging.MessagingCampaignAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignAuditIngestionService {

    private final MessagingCampaignAuditRepository repository;

    public void handleCreated(CampaignCreatedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.CAMPAIGN_CREATED)
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .templateId(req.getTemplateId())
                .totalPhoneNumbers(req.getTotalPhoneNumbers())
                .mediaCount(req.getMediaCount())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleScheduled(CampaignScheduledIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.CAMPAIGN_SCHEDULED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .scheduledAt(req.getScheduledAt())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleRescheduled(CampaignRescheduledIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.CAMPAIGN_RESCHEDULED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .scheduledAt(req.getNewScheduledAt())
                .previousScheduledAt(req.getPreviousScheduledAt())
                .isReschedule(true)
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handlePreparationStarted(PreparationStartedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.PREPARATION_STARTED)
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handlePreparationFailed(PreparationFailedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.PREPARATION_FAILED)
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .failureStage(req.getFailureStage())
                .failureReason(req.getFailureReason())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleDispatchStarted(DispatchStartedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.DISPATCH_STARTED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .isRecovery(req.getIsRecovery())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handlePaused(CampaignPausedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.CAMPAIGN_PAUSED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleResumed(CampaignResumedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.CAMPAIGN_RESUMED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleCancelled(CampaignCancelledIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.CAMPAIGN_CANCELLED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .cancelledAtStage(req.getCancelledAtStage())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleCompleted(CampaignCompletedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.CAMPAIGN_COMPLETED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .sentCount(req.getSentCount())
                .failedCount(req.getFailedCount())
                .skippedCount(req.getSkippedCount())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    public void handleFailed(CampaignFailedIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        repository.save(MessagingCampaignAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(CampaignAuditEventType.CAMPAIGN_FAILED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .campaignId(req.getCampaignId())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaAccountId(req.getWabaAccountId())
                .actorType(req.getActorType())
                .failureStage(req.getFailureStage())
                .failureReason(req.getFailureReason())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now())
                .build());
    }

    private boolean isDuplicate(String eventId) {
        if (repository.existsByEventId(eventId)) {
            log.warn("Duplicate campaign audit event ignored: eventId={}", eventId);
            return true;
        }
        return false;
    }
}