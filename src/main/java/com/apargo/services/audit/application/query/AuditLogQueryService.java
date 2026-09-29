package com.apargo.services.audit.application.query;

import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.apargo.platform.contract.audit.AuditChannel;
import com.apargo.platform.contract.audit.AuditEventStatus;
import com.apargo.platform.contract.event.EventIds;
import com.apargo.services.audit.application.port.out.AuditLogReader;
import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.ErrorCode;
import com.apargo.services.audit.domain.model.AuditLog;

/** Audit log reads: validated, time-bounded, cursor-paged. */
public final class AuditLogQueryService {

    private static final Set<String> STATUSES = names(AuditEventStatus.values());
    private static final Set<String> CHANNELS = names(AuditChannel.values());

    private final AuditLogReader reader;
    private final QuerySettings settings;
    private final Clock clock;

    public AuditLogQueryService(AuditLogReader reader, QuerySettings settings, Clock clock) {
        this.reader = Objects.requireNonNull(reader);
        this.settings = Objects.requireNonNull(settings);
        this.clock = Objects.requireNonNull(clock);
    }

    public CursorResult<AuditLog> search(AuditLogFilter filter, String cursor, Integer size) {
        FilterRules.requireScope(filter.orgId(), "X-Org-Id is required unless searching by trace id", filter.traceId());
        new FilterRules()
                .upperSnake("module", filter.module())
                .upperSnake("eventType", filter.eventType())
                .oneOf("status", filter.status(), STATUSES)
                .oneOf("channel", filter.channel(), CHANNELS)
                .maxLength("actorId", filter.actorId())
                .upperSnake("entityType", filter.entityType())
                .maxLength("entityId", filter.entityId())
                .requiredWith("entityType", filter.entityType(), "entityId", filter.entityId())
                .maxLength("sourceService", filter.sourceService())
                .maxLength("errorCode", filter.errorCode())
                .traceId("traceId", filter.traceId())
                .throwIfInvalid();

        int limit = settings.resolvePageSize(size);
        PageCursor after = CursorCodec.decode(cursor);
        TimeRange range = TimeRange.resolve(filter.from(), filter.to(), clock.instant(), settings);

        Slice<AuditLog> slice = reader.search(filter.withRange(range), after, limit);
        return new CursorResult<>(slice.items(), limit, nextCursor(slice));
    }

    /**
     * @param orgId     tenant scope; {@code null} only for internal callers
     * @param projectId project scope from {@code X-Project-Id}; a log of another project is "not found"
     */
    public AuditLog get(String eventId, Long orgId, Long projectId) {
        if (!EventIds.isUuidV7(eventId)) {
            throw new ApiException(ErrorCode.AUDIT_LOG_NOT_FOUND);
        }
        return reader.findById(eventId, orgId)
                .filter(log -> projectId == null || projectId.equals(log.projectId()))
                .orElseThrow(() -> new ApiException(ErrorCode.AUDIT_LOG_NOT_FOUND));
    }

    private static String nextCursor(Slice<AuditLog> slice) {
        if (!slice.hasMore() || slice.items().isEmpty()) {
            return null;
        }
        AuditLog last = slice.items().get(slice.items().size() - 1);
        return CursorCodec.encode(new PageCursor(last.occurredAt(), last.eventId()));
    }

    private static Set<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }
}
