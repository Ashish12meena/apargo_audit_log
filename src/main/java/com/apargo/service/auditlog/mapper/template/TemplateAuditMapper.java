package com.apargo.service.auditlog.mapper.template;

import com.apargo.service.auditlog.dto.request.ingest.template.*;
import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateAuditIngestRequest;
import com.apargo.service.auditlog.dto.request.ingest.template.base.BaseTemplateSyncAuditIngestRequest;
import org.springframework.stereotype.Component;

@Component
public class TemplateAuditMapper {

    // ── Flat → Typed DTO (lifecycle) ──────────────────────────────────────────

    public TemplateCreatedIngestRequest toCreated(TemplateAuditIngestRequest req) {
        TemplateCreatedIngestRequest dto = new TemplateCreatedIngestRequest();
        copyBase(req, dto);
        dto.setToStatus(req.getToStatus());
        dto.setIsDraft(req.getIsDraft());
        return dto;
    }

    public TemplateSubmittedIngestRequest toSubmitted(TemplateAuditIngestRequest req) {
        TemplateSubmittedIngestRequest dto = new TemplateSubmittedIngestRequest();
        copyBase(req, dto);
        dto.setFromStatus(req.getFromStatus());
        dto.setToStatus(req.getToStatus());
        return dto;
    }

    public TemplateUpdatedIngestRequest toUpdated(TemplateAuditIngestRequest req) {
        TemplateUpdatedIngestRequest dto = new TemplateUpdatedIngestRequest();
        copyBase(req, dto);
        dto.setFromStatus(req.getFromStatus());
        dto.setToStatus(req.getToStatus());
        return dto;
    }

    public TemplateDeletedIngestRequest toDeleted(TemplateAuditIngestRequest req) {
        TemplateDeletedIngestRequest dto = new TemplateDeletedIngestRequest();
        copyBase(req, dto);
        dto.setDeletedFromMeta(req.getDeletedFromMeta());
        return dto;
    }

    public TemplateBulkDeletedIngestRequest toBulkDeleted(TemplateAuditIngestRequest req) {
        TemplateBulkDeletedIngestRequest dto = new TemplateBulkDeletedIngestRequest();
        copyBase(req, dto);
        dto.setDeletedCount(req.getDeletedCount());
        return dto;
    }

    public TemplateApprovedIngestRequest toApproved(TemplateAuditIngestRequest req) {
        TemplateApprovedIngestRequest dto = new TemplateApprovedIngestRequest();
        copyBase(req, dto);
        dto.setFromStatus(req.getFromStatus());
        dto.setToStatus(req.getToStatus());
        return dto;
    }

    public TemplateRejectedIngestRequest toRejected(TemplateAuditIngestRequest req) {
        TemplateRejectedIngestRequest dto = new TemplateRejectedIngestRequest();
        copyBase(req, dto);
        dto.setFromStatus(req.getFromStatus());
        dto.setToStatus(req.getToStatus());
        dto.setRejectionReason(req.getRejectionReason());
        return dto;
    }

    public TemplatePausedIngestRequest toPaused(TemplateAuditIngestRequest req) {
        TemplatePausedIngestRequest dto = new TemplatePausedIngestRequest();
        copyBase(req, dto);
        dto.setFromStatus(req.getFromStatus());
        dto.setToStatus(req.getToStatus());
        dto.setRejectionReason(req.getRejectionReason());
        return dto;
    }

    public TemplateDisabledIngestRequest toDisabled(TemplateAuditIngestRequest req) {
        TemplateDisabledIngestRequest dto = new TemplateDisabledIngestRequest();
        copyBase(req, dto);
        dto.setFromStatus(req.getFromStatus());
        dto.setToStatus(req.getToStatus());
        return dto;
    }

    public TemplateCategoryChangedIngestRequest toCategoryChanged(TemplateAuditIngestRequest req) {
        TemplateCategoryChangedIngestRequest dto = new TemplateCategoryChangedIngestRequest();
        copyBase(req, dto);
        dto.setPreviousCategory(req.getPreviousCategory());
        dto.setNewCategory(req.getNewCategory());
        return dto;
    }

    // ── Flat → Typed DTO (sync) ───────────────────────────────────────────────

    public TemplateSyncStartedIngestRequest toSyncStarted(TemplateSyncAuditIngestRequest req) {
        TemplateSyncStartedIngestRequest dto = new TemplateSyncStartedIngestRequest();
        copySyncBase(req, dto);
        return dto;
    }

    public TemplateSyncCompletedIngestRequest toSyncCompleted(TemplateSyncAuditIngestRequest req) {
        TemplateSyncCompletedIngestRequest dto = new TemplateSyncCompletedIngestRequest();
        copySyncBase(req, dto);
        dto.setInsertedCount(req.getInsertedCount());
        dto.setUpdatedCount(req.getUpdatedCount());
        dto.setDeletedCount(req.getDeletedCount());
        dto.setSkippedCount(req.getSkippedCount());
        dto.setDurationMs(req.getDurationMs());
        return dto;
    }

    public TemplateSyncFailedIngestRequest toSyncFailed(TemplateSyncAuditIngestRequest req) {
        TemplateSyncFailedIngestRequest dto = new TemplateSyncFailedIngestRequest();
        copySyncBase(req, dto);
        dto.setFailureReason(req.getFailureReason());
        return dto;
    }

    // ── Base copy helpers ─────────────────────────────────────────────────────

    private void copyBase(
            TemplateAuditIngestRequest src,
            BaseTemplateAuditIngestRequest dst) {
        dst.setEventId(src.getEventId());
        dst.setOrgId(src.getOrgId());
        dst.setProjectId(src.getProjectId());
        dst.setWabaId(src.getWabaId());
        dst.setTemplateId(src.getTemplateId());
        dst.setActorType(src.getActorType());
        dst.setActorId(src.getActorId());
        dst.setOccurredAt(src.getOccurredAt());
    }

    private void copySyncBase(
            TemplateSyncAuditIngestRequest src,
            BaseTemplateSyncAuditIngestRequest dst) {
        dst.setEventId(src.getEventId());
        dst.setOrgId(src.getOrgId());
        dst.setProjectId(src.getProjectId());
        dst.setWabaId(src.getWabaId());
        dst.setActorType(src.getActorType());
        dst.setActorId(src.getActorId());
        dst.setOccurredAt(src.getOccurredAt());
    }
}