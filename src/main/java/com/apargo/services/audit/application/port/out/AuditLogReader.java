package com.apargo.services.audit.application.port.out;

import java.util.List;
import java.util.Optional;

import com.apargo.services.audit.application.query.AuditLogFilter;
import com.apargo.services.audit.application.query.PageCursor;
import com.apargo.services.audit.application.query.Slice;
import com.apargo.services.audit.application.query.StatsQuery;
import com.apargo.services.audit.application.query.StatsRow;
import com.apargo.services.audit.domain.model.AuditLog;

/** Read side of {@code audit_logs}. Results are ordered by occurredAt desc, then eventId desc. */
public interface AuditLogReader {

    Slice<AuditLog> search(AuditLogFilter filter, PageCursor after, int limit);

    /** @param orgId tenant scope, or {@code null} for an unscoped (internal) lookup */
    Optional<AuditLog> findById(String eventId, Long orgId);

    List<StatsRow> stats(StatsQuery query, int maxRows);
}
