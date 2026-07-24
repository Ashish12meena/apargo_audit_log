package com.apargo.service.auditlog.dto.request;

import com.apargo.service.auditlog.enums.AuditEventStatus;
import com.apargo.service.auditlog.enums.AuditEventType;
import com.apargo.service.auditlog.enums.Device;
import com.apargo.service.auditlog.enums.Module;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * Backing object for GET /api/v1/audit-logs, populated via @ModelAttribute.
 * Every field is optional — an unset field means "don't filter on this".
 */
@Data
public class AuditSearchRequest {

    private Integer page;
    private Integer limit;

    private String search; // nullable, free-text over entityType/errorMessage

    @NotNull(message = "organizationId is required")
    private Long organizationId;

    private Long projectId;
    private Long userId; // -> actorId

    private Module module;
    private AuditEventType action; // -> eventType
    private String entityType;
    private String entityId;
    private AuditEventStatus status; // -> eventStatus
    private Device device;

    private Instant fromDate;
    private Instant toDate;

    private String sortBy;
    private String sortOrder;
}
