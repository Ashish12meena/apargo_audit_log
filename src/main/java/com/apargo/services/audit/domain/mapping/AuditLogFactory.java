package com.apargo.services.audit.domain.mapping;

import java.time.Instant;
import java.util.Objects;

import com.apargo.platform.contract.audit.AuditEventDto;
import com.apargo.services.audit.common.enums.LogStream;
import com.apargo.services.audit.domain.model.AuditLog;
import com.apargo.services.audit.domain.policy.RetentionPolicy;

/** Turns an accepted {@link AuditEventDto} into the stored {@link AuditLog}. Names are not changed. */
public final class AuditLogFactory {

    private final RetentionPolicy retentionPolicy;

    public AuditLogFactory(RetentionPolicy retentionPolicy) {
        this.retentionPolicy = Objects.requireNonNull(retentionPolicy, "retentionPolicy");
    }

    /** Call only for events the rules accepted (required fields are non-null). */
    public AuditLog create(AuditEventDto event, Instant recordedAt) {
        return new AuditLog(
                event.eventId(),
                event.schemaVersion(),
                event.sourceService(),
                event.module(),
                event.eventType(),
                event.status(),
                event.orgId(),
                event.projectId(),
                event.actor(),
                event.entity(),
                event.changes(),
                event.metadata(),
                event.error(),
                event.channel(),
                event.requestId(),
                event.traceId(),
                event.ip(),
                event.userAgent(),
                event.occurredAt(),
                recordedAt,
                retentionPolicy.archiveAt(LogStream.AUDIT, recordedAt));
    }
}
