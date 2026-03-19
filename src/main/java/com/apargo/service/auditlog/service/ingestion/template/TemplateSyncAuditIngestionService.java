package com.apargo.service.auditlog.service.ingestion.template;

import com.apargo.service.auditlog.dto.request.ingest.template.*;
import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateSyncAuditIngestRequest;
import com.apargo.service.auditlog.enums.TemplateAuditEventType;
import com.apargo.service.auditlog.model.template.TemplateSyncAuditEvent;
import com.apargo.service.auditlog.repository.template.TemplateSyncAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateSyncAuditIngestionService {

    private final TemplateSyncAuditRepository repository;

    public void handleSyncStarted(TemplateSyncStartedIngestRequest req) {
        save(syncBaseBuilder(req, TemplateAuditEventType.TEMPLATE_SYNC_STARTED)
                .build());
    }

    public void handleSyncCompleted(TemplateSyncCompletedIngestRequest req) {
        save(syncBaseBuilder(req, TemplateAuditEventType.TEMPLATE_SYNC_COMPLETED)
                .insertedCount(req.getInsertedCount())
                .updatedCount(req.getUpdatedCount())
                .deletedCount(req.getDeletedCount())
                .skippedCount(req.getSkippedCount())
                .durationMs(req.getDurationMs())
                .build());
    }

    public void handleSyncFailed(TemplateSyncFailedIngestRequest req) {
        save(syncBaseBuilder(req, TemplateAuditEventType.TEMPLATE_SYNC_FAILED)
                .failureReason(req.getFailureReason())
                .build());
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private TemplateSyncAuditEvent.TemplateSyncAuditEventBuilder syncBaseBuilder(
            BaseTemplateSyncAuditIngestRequest req,
            TemplateAuditEventType eventType) {
        return TemplateSyncAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(eventType)
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaId(req.getWabaId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now());
    }

    private void save(TemplateSyncAuditEvent event) {
        repository.save(event);
        log.info("Template sync audit event saved: eventType={} wabaId={} eventId={}",
                event.getEventType(), event.getWabaId(), event.getEventId());
    }
}