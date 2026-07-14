package com.apargo.service.auditlog.dto.response;

import com.apargo.service.auditlog.enums.AuditActorType;
import com.apargo.service.auditlog.enums.AuditEventStatus;
import com.apargo.service.auditlog.enums.AuditEventType;
import com.apargo.service.auditlog.enums.Device;
import com.apargo.service.auditlog.enums.Module;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class AuditEventResponse {

    private String id;
    private String eventId;

    private Module module;
    private AuditEventType eventType;
    private AuditEventStatus eventStatus;

    private Long orgId;
    private Long projectId;
    private String wabaId;

    private AuditActorType actorType;
    private Long actorId;

    private String entityType;
    private String entityId;

    private String requestId;
    private String traceId;
    private String ipAddress;
    private String userAgent;
    private Device device;

    private Map<String, Object> oldValue;
    private Map<String, Object> newValue;
    private Map<String, Object> metadata;

    private String errorMessage;

    private Instant occurredAt;
    private Instant recordedAt;
}
