package com.apargo.service.auditlog.service.ingestion.template;

import com.apargo.service.auditlog.dto.request.ingest.template.TemplateSyncAuditIngestRequest;
import com.apargo.service.auditlog.mapper.template.TemplateAuditMapper;
import com.apargo.service.auditlog.model.template.TemplateSyncAuditEvent;
import com.apargo.service.auditlog.repository.template.TemplateSyncAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateSyncAuditIngestionService {

    private final TemplateSyncAuditRepository repository;
    private final TemplateAuditMapper mapper;

    public void ingest(TemplateSyncAuditIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        TemplateSyncAuditEvent event = mapper.toSyncEvent(req);
        repository.save(event);

        log.info("Template sync audit event saved: eventType={} wabaId={} eventId={}",
                req.getEventType(), req.getWabaId(), req.getEventId());
    }

    private boolean isDuplicate(String eventId) {
        if (repository.existsByEventId(eventId)) {
            log.warn("Duplicate template sync audit event ignored: eventId={}", eventId);
            return true;
        }
        return false;
    }
}