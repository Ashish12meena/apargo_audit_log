package com.apargo.service.auditlog.service.command;

import com.apargo.service.auditlog.dto.request.AuditIngestRequest;
import com.apargo.service.auditlog.dto.response.AuditEventResponse;
import com.apargo.service.auditlog.exception.DuplicateAuditEventException;
import com.apargo.service.auditlog.mapper.AuditLogMapper;
import com.apargo.service.auditlog.model.AuditLog;
import com.apargo.service.auditlog.repository.write.AuditLogWriteRepository;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The one and only place an AuditLog row gets written. Both
 * AuditIngestController (REST) and AuditKafkaConsumer (Kafka) call this
 * same method — neither entry point talks to the repository directly, so
 * idempotency handling, mapping, and logging behave identically regardless
 * of transport.
 */
@Service
@RequiredArgsConstructor
public class AuditCommandService {

    private static final Logger log = LoggerFactory.getLogger(AuditCommandService.class);

    private final AuditLogWriteRepository writeRepository;
    private final AuditLogMapper mapper;

    public AuditEventResponse ingest(AuditIngestRequest request) {
        AuditLog document = mapper.toDocument(request);

        try {
            AuditLog saved = writeRepository.insert(document);
            log.info("audit_ingested eventId={} module={} eventType={} orgId={} projectId={} entityType={} entityId={} status={}",
                    saved.getEventId(), saved.getModule(), saved.getEventType(), saved.getOrgId(),
                    saved.getProjectId(), saved.getEntityType(), saved.getEntityId(), saved.getEventStatus());
            return mapper.toResponse(saved);
        } catch (DuplicateKeyException e) {
            log.warn("audit_duplicate_rejected eventId={} module={} eventType={}",
                    document.getEventId(), document.getModule(), document.getEventType());
            throw new DuplicateAuditEventException(document.getEventId());
        }
    }
}
