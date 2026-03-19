package com.apargo.service.auditlog.mapper.template;

import com.apargo.service.auditlog.dto.request.ingest.template.TemplateAuditIngestRequest;
import com.apargo.service.auditlog.dto.request.ingest.template.TemplateSyncAuditIngestRequest;
import com.apargo.service.auditlog.model.template.TemplateAuditEvent;
import com.apargo.service.auditlog.model.template.TemplateSyncAuditEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TemplateAuditMapper {

    public TemplateAuditEvent toEvent(TemplateAuditIngestRequest req) {
        TemplateAuditEvent.TemplateAuditEventBuilder builder = TemplateAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(req.getEventType())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaId(req.getWabaId())
                .templateId(req.getTemplateId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now());

        // enrich builder based on eventType
        switch (req.getEventType()) {

            case TEMPLATE_CREATED -> builder
                    .toStatus(req.getToStatus())
                    .isDraft(req.getIsDraft());

            case TEMPLATE_SUBMITTED,
                 TEMPLATE_UPDATED,
                 TEMPLATE_APPROVED,
                 TEMPLATE_DISABLED -> builder
                    .fromStatus(req.getFromStatus())
                    .toStatus(req.getToStatus());

            case TEMPLATE_REJECTED -> builder
                    .fromStatus(req.getFromStatus())
                    .toStatus(req.getToStatus())
                    .rejectionReason(req.getRejectionReason());

            case TEMPLATE_PAUSED -> builder
                    .fromStatus(req.getFromStatus())
                    .toStatus(req.getToStatus())
                    .rejectionReason(req.getRejectionReason());

            case TEMPLATE_DELETED -> builder
                    .deletedFromMeta(req.getDeletedFromMeta());

            case TEMPLATE_BULK_DELETED -> builder
                    .deletedCount(req.getDeletedCount());

            case TEMPLATE_CATEGORY_CHANGED -> builder
                    .previousCategory(req.getPreviousCategory())
                    .newCategory(req.getNewCategory());
        }

        return builder.build();
    }

    public TemplateSyncAuditEvent toSyncEvent(TemplateSyncAuditIngestRequest req) {
        TemplateSyncAuditEvent.TemplateSyncAuditEventBuilder builder = TemplateSyncAuditEvent.builder()
                .eventId(req.getEventId())
                .eventType(req.getEventType())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .wabaId(req.getWabaId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now());

        switch (req.getEventType()) {

            case TEMPLATE_SYNC_STARTED -> {
                // no extra fields
            }

            case TEMPLATE_SYNC_COMPLETED -> builder
                    .insertedCount(req.getInsertedCount())
                    .updatedCount(req.getUpdatedCount())
                    .deletedCount(req.getDeletedCount())
                    .skippedCount(req.getSkippedCount())
                    .durationMs(req.getDurationMs());

            case TEMPLATE_SYNC_FAILED -> builder
                    .failureReason(req.getFailureReason());
        }

        return builder.build();
    }
}