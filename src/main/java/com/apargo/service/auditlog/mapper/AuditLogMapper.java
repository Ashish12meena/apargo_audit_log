package com.apargo.service.auditlog.mapper;

import com.apargo.service.auditlog.dto.request.AuditIngestRequest;
import com.apargo.service.auditlog.dto.response.AuditEventResponse;
import com.apargo.service.auditlog.model.AuditLog;
import com.apargo.service.auditlog.util.IdempotencyUtil;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * One mapper for the whole service instead of one per module. Two
 * directions only: ingest request -> document (on write), document ->
 * response (on read). If this class ever grows unwieldy, split by direction
 * (AuditRequestMapper / AuditResponseMapper) rather than by module again.
 */
@Component
public class AuditLogMapper {

    public AuditLog toDocument(AuditIngestRequest req) {
        String resolvedEventId = IdempotencyUtil.resolveEventId(
                req.getEventId(),
                req.getModule(),
                req.getEventType(),
                req.getOrgId(),
                req.getEntityType(),
                req.getEntityId(),
                req.getOccurredAt().toEpochMilli()
        );

        return AuditLog.builder()
                .eventId(resolvedEventId)
                .module(req.getModule())
                .eventType(req.getEventType())
                .eventStatus(req.getEventStatus())
                .orgId(req.getOrgId())
                .projectId(req.getProjectId())
                .actorType(req.getActorType())
                .actorId(req.getActorId())
                .entityType(req.getEntityType())
                .entityId(req.getEntityId())
                .requestId(req.getRequestId())
                .traceId(req.getTraceId())
                .ipAddress(req.getIpAddress())
                .device(req.getDevice())
                .userAgent(req.getUserAgent())
                .oldValue(req.getOldValue())
                .newValue(req.getNewValue())
                .metadata(req.getMetadata() == null ? java.util.Map.of() : req.getMetadata())
                .errorMessage(req.getErrorMessage())
                .occurredAt(req.getOccurredAt())
                .recordedAt(Instant.now()) // always server-generated, never client-supplied
                .build();
    }

    public AuditEventResponse toResponse(AuditLog doc) {
        return AuditEventResponse.builder()
                .id(doc.getId())
                .eventId(doc.getEventId())
                .module(doc.getModule())
                .eventType(doc.getEventType())
                .eventStatus(doc.getEventStatus())
                .orgId(doc.getOrgId())
                .projectId(doc.getProjectId())
                .actorType(doc.getActorType())
                .actorId(doc.getActorId())
                .entityType(doc.getEntityType())
                .entityId(doc.getEntityId())
                .requestId(doc.getRequestId())
                .traceId(doc.getTraceId())
                .ipAddress(doc.getIpAddress())
                .device(doc.getDevice())
                .userAgent(doc.getUserAgent())
                .oldValue(doc.getOldValue())
                .newValue(doc.getNewValue())
                .metadata(doc.getMetadata())
                .errorMessage(doc.getErrorMessage())
                .occurredAt(doc.getOccurredAt())
                .recordedAt(doc.getRecordedAt())
                .build();
    }
}
