package com.apargo.services.audit.application.query;

import java.time.Clock;
import java.util.Objects;

import com.apargo.platform.contract.event.EventIds;
import com.apargo.services.audit.application.port.out.AccessLogReader;
import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.ErrorCode;
import com.apargo.services.audit.domain.model.AccessLog;

/** Access log reads: validated, time-bounded, cursor-paged. */
public final class AccessLogQueryService {

    private final AccessLogReader reader;
    private final QuerySettings settings;
    private final Clock clock;

    public AccessLogQueryService(AccessLogReader reader, QuerySettings settings, Clock clock) {
        this.reader = Objects.requireNonNull(reader);
        this.settings = Objects.requireNonNull(settings);
        this.clock = Objects.requireNonNull(clock);
    }

    public CursorResult<AccessLog> search(AccessLogFilter filter, String cursor, Integer size) {
        FilterRules.requireScope(filter.orgId(), "X-Org-Id is required unless searching by trace id or actor id",
                filter.traceId(), filter.actorId());
        new FilterRules()
                .upperSnake("eventType", filter.eventType())
                .upperSnake("status", filter.status())
                .maxLength("actorId", filter.actorId())
                .traceId("traceId", filter.traceId())
                .throwIfInvalid();

        int limit = settings.resolvePageSize(size);
        PageCursor after = CursorCodec.decode(cursor);
        TimeRange range = TimeRange.resolve(filter.from(), filter.to(), clock.instant(), settings);

        Slice<AccessLog> slice = reader.search(filter.withRange(range), after, limit);
        String next = null;
        if (slice.hasMore() && !slice.items().isEmpty()) {
            AccessLog last = slice.items().get(slice.items().size() - 1);
            next = CursorCodec.encode(new PageCursor(last.occurredAt(), last.eventId()));
        }
        return new CursorResult<>(slice.items(), limit, next);
    }

    /** @param orgId tenant scope; {@code null} only for internal callers */
    public AccessLog get(String eventId, Long orgId) {
        if (!EventIds.isUuidV7(eventId)) {
            throw new ApiException(ErrorCode.ACCESS_LOG_NOT_FOUND);
        }
        return reader.findById(eventId, orgId).orElseThrow(() -> new ApiException(ErrorCode.ACCESS_LOG_NOT_FOUND));
    }
}
