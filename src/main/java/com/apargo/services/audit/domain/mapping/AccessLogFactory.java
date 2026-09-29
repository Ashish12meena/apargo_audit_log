package com.apargo.services.audit.domain.mapping;

import java.time.Instant;
import java.util.Objects;

import com.apargo.services.audit.common.enums.LogStream;
import com.apargo.services.audit.domain.event.AccessEventMessage;
import com.apargo.services.audit.domain.model.AccessLog;
import com.apargo.services.audit.domain.policy.RetentionPolicy;

/** Turns an accepted {@link AccessEventMessage} into the stored {@link AccessLog}. */
public final class AccessLogFactory {

    private final RetentionPolicy retentionPolicy;

    public AccessLogFactory(RetentionPolicy retentionPolicy) {
        this.retentionPolicy = Objects.requireNonNull(retentionPolicy, "retentionPolicy");
    }

    /** Call only for events the rules accepted (required fields are non-null). */
    public AccessLog create(AccessEventMessage event, Instant recordedAt) {
        return new AccessLog(
                event.eventId(),
                event.schemaVersion(),
                event.sourceService(),
                event.eventType(),
                event.status(),
                event.orgId(),
                event.actor(),
                event.authMethod(),
                event.failureCode(),
                event.resource(),
                event.ip(),
                event.userAgent(),
                event.country(),
                event.metadata(),
                event.requestId(),
                event.traceId(),
                event.occurredAt(),
                recordedAt,
                retentionPolicy.archiveAt(LogStream.ACCESS, recordedAt));
    }
}
