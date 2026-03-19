package com.apargo.service.auditlog.service.ingestion.template;

import com.apargo.service.auditlog.dto.request.ingest.template.TemplateAuditIngestRequest;
import com.apargo.service.auditlog.mapper.template.TemplateAuditMapper;
import com.apargo.service.auditlog.model.template.TemplateAuditEvent;
import com.apargo.service.auditlog.repository.template.TemplateAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateAuditIngestionService {

    private final TemplateAuditRepository repository;
    private final TemplateAuditMapper mapper;

    public void ingest(TemplateAuditIngestRequest req) {
        if (isDuplicate(req.getEventId())) return;

        TemplateAuditEvent event = mapper.toEvent(req);
        repository.save(event);

        log.info("Template audit event saved: eventType={} templateId={} eventId={}",
                req.getEventType(), req.getTemplateId(), req.getEventId());
    }

    private boolean isDuplicate(String eventId) {
        if (repository.existsByEventId(eventId)) {
            log.warn("Duplicate template audit event ignored: eventId={}", eventId);
            return true;
        }
        return false;
    }
}