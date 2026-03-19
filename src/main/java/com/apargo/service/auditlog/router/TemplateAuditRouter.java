package com.apargo.service.auditlog.router;



import com.apargo.service.auditlog.dto.request.ingest.template.*;
import com.apargo.service.auditlog.mapper.template.TemplateAuditMapper;
import com.apargo.service.auditlog.service.ingestion.template.TemplateAuditIngestionService;
import com.apargo.service.auditlog.service.ingestion.template.TemplateSyncAuditIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateAuditRouter {

    private final TemplateAuditMapper mapper;
    private final TemplateAuditIngestionService templateService;
    private final TemplateSyncAuditIngestionService syncService;

    public void route(TemplateAuditIngestRequest req) {
        switch (req.getEventType()) {
            case TEMPLATE_CREATED          -> templateService.handleCreated(mapper.toCreated(req));
            case TEMPLATE_SUBMITTED        -> templateService.handleSubmitted(mapper.toSubmitted(req));
            case TEMPLATE_UPDATED          -> templateService.handleUpdated(mapper.toUpdated(req));
            case TEMPLATE_DELETED          -> templateService.handleDeleted(mapper.toDeleted(req));
            case TEMPLATE_BULK_DELETED     -> templateService.handleBulkDeleted(mapper.toBulkDeleted(req));
            case TEMPLATE_APPROVED         -> templateService.handleApproved(mapper.toApproved(req));
            case TEMPLATE_REJECTED         -> templateService.handleRejected(mapper.toRejected(req));
            case TEMPLATE_PAUSED           -> templateService.handlePaused(mapper.toPaused(req));
            case TEMPLATE_DISABLED         -> templateService.handleDisabled(mapper.toDisabled(req));
            case TEMPLATE_CATEGORY_CHANGED -> templateService.handleCategoryChanged(mapper.toCategoryChanged(req));
            default -> log.warn("Unhandled template audit eventType={}", req.getEventType());
        }
    }

    public void routeSync(TemplateSyncAuditIngestRequest req) {
        switch (req.getEventType()) {
            case TEMPLATE_SYNC_STARTED   -> syncService.handleSyncStarted(mapper.toSyncStarted(req));
            case TEMPLATE_SYNC_COMPLETED -> syncService.handleSyncCompleted(mapper.toSyncCompleted(req));
            case TEMPLATE_SYNC_FAILED    -> syncService.handleSyncFailed(mapper.toSyncFailed(req));
            default -> log.warn("Unhandled template sync audit eventType={}", req.getEventType());
        }
    }
}