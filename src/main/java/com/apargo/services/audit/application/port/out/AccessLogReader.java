package com.apargo.services.audit.application.port.out;

import java.util.Optional;

import com.apargo.services.audit.application.query.AccessLogFilter;
import com.apargo.services.audit.application.query.PageCursor;
import com.apargo.services.audit.application.query.Slice;
import com.apargo.services.audit.domain.model.AccessLog;

/** Read side of {@code access_logs}. Results are ordered by occurredAt desc, then eventId desc. */
public interface AccessLogReader {

    Slice<AccessLog> search(AccessLogFilter filter, PageCursor after, int limit);

    /** @param orgId tenant scope, or {@code null} for an unscoped (internal) lookup */
    Optional<AccessLog> findById(String eventId, Long orgId);
}
