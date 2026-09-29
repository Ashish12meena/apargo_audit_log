package com.apargo.services.audit.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.apargo.platform.contract.audit.AuditActorDto;
import com.apargo.platform.contract.audit.AuditChangeDto;
import com.apargo.platform.contract.audit.AuditChannel;
import com.apargo.platform.contract.audit.AuditEntityDto;
import com.apargo.platform.contract.audit.AuditErrorDto;
import com.apargo.platform.contract.audit.AuditEventStatus;
import com.apargo.services.audit.common.enums.LogStream;

/**
 * One {@code audit_logs} document: the contract's {@code AuditEventDto} field-for-field
 * (minus {@code environment}), plus {@code recordedAt} and {@code archiveAt} set by this service.
 */
public record AuditLog(
        String eventId,
        int schemaVersion,
        String sourceService,
        String module,
        String eventType,
        AuditEventStatus status,
        long orgId,
        Long projectId,
        AuditActorDto actor,
        AuditEntityDto entity,
        List<AuditChangeDto> changes,
        Map<String, Object> metadata,
        AuditErrorDto error,
        AuditChannel channel,
        String requestId,
        String traceId,
        String ip,
        String userAgent,
        Instant occurredAt,
        Instant recordedAt,
        Instant archiveAt) implements LogRecord {

    public AuditLog {
        changes = changes == null ? List.of() : List.copyOf(changes);
        metadata = metadata == null ? Map.of() : metadata;
    }

    @Override
    public LogStream stream() {
        return LogStream.AUDIT;
    }
}
