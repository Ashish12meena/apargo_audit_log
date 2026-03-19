package com.apargo.service.auditlog.service.ingestion.template;

import com.apargo.service.auditlog.dto.request.ingest.template.*;
import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateAuditIngestRequest;
import com.apargo.service.auditlog.enums.TemplateAuditEventType;
import com.apargo.service.auditlog.model.template.TemplateAuditEvent;
import com.apargo.service.auditlog.repository.template.TemplateAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateAuditIngestionService {

    private final TemplateAuditRepository repository;

    public void handleCreated(TemplateCreatedIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_CREATED)
                .toStatus(req.getToStatus())
                .isDraft(req.getIsDraft())
                .build());
    }

    public void handleSubmitted(TemplateSubmittedIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_SUBMITTED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .build());
    }

    public void handleUpdated(TemplateUpdatedIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_UPDATED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .build());
    }

    public void handleDeleted(TemplateDeletedIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_DELETED)
                .deletedFromMeta(req.getDeletedFromMeta())
                .build());
    }

    public void handleBulkDeleted(TemplateBulkDeletedIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_BULK_DELETED)
                .deletedCount(req.getDeletedCount())
                .build());
    }

    public void handleApproved(TemplateApprovedIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_APPROVED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .build());
    }

    public void handleRejected(TemplateRejectedIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_REJECTED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .rejectionReason(req.getRejectionReason())
                .build());
    }

    public void handlePaused(TemplatePausedIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_PAUSED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .rejectionReason(req.getRejectionReason())
                .build());
    }

    public void handleDisabled(TemplateDisabledIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_DISABLED)
                .fromStatus(req.getFromStatus())
                .toStatus(req.getToStatus())
                .build());
    }

    public void handleCategoryChanged(TemplateCategoryChangedIngestRequest req) {
        save(baseBuilder(req, TemplateAuditEventType.TEMPLATE_CATEGORY_CHANGED)
                .previousCategory(req.getPreviousCategory())
                .newCategory(req.getNewCategory())
                .build());
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private TemplateAuditEvent.TemplateAuditEventBuilder baseBuilder(
            BaseTemplateAuditIngestRequest req,
            TemplateAuditEventType eventType) {
        return TemplateAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(eventType)
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaId(req.getWabaId())
                .templateId(req.getTemplateId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now());
    }

    private void save(TemplateAuditEvent event) {
        repository.save(event);
        log.info("Template audit event saved: eventType={} templateId={} eventId={}",
                event.getEventType(), event.getTemplateId(), event.getEventId());
    }
}