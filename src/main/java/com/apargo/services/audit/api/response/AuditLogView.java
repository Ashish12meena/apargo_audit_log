package com.apargo.services.audit.api.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.apargo.platform.contract.audit.AuditActorDto;
import com.apargo.platform.contract.audit.AuditChangeDto;
import com.apargo.platform.contract.audit.AuditChannel;
import com.apargo.platform.contract.audit.AuditEntityDto;
import com.apargo.platform.contract.audit.AuditErrorDto;
import com.apargo.platform.contract.audit.AuditEventStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/** One audit log in API responses. Field names are the contract's. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditLogView(
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
        Instant recordedAt) {
}
