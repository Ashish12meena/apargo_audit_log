package com.apargo.services.audit.api.mapper;

import com.apargo.services.audit.api.response.AccessLogView;
import com.apargo.services.audit.api.response.AuditLogView;
import com.apargo.services.audit.api.response.AuditStatsView;
import com.apargo.services.audit.application.query.CursorResult;
import com.apargo.services.audit.application.query.StatsResult;
import com.apargo.services.audit.common.response.CursorPage;
import com.apargo.services.audit.domain.model.AccessLog;
import com.apargo.services.audit.domain.model.AuditLog;

/** Domain → API views. {@code archiveAt} is internal and never exposed. */
public final class ApiViewMapper {

    private ApiViewMapper() {
    }

    public static AuditLogView toView(AuditLog log) {
        return new AuditLogView(log.eventId(), log.schemaVersion(), log.sourceService(), log.module(),
                log.eventType(), log.status(), log.orgId(), log.projectId(), log.actor(), log.entity(),
                log.changes(), log.metadata(), log.error(), log.channel(), log.requestId(), log.traceId(),
                log.ip(), log.userAgent(), log.occurredAt(), log.recordedAt());
    }

    public static AccessLogView toView(AccessLog log) {
        return new AccessLogView(log.eventId(), log.schemaVersion(), log.sourceService(), log.eventType(),
                log.status(), log.orgId(), log.actor(), log.authMethod(), log.failureCode(), log.resource(),
                log.ip(), log.userAgent(), log.country(), log.metadata(), log.requestId(), log.traceId(),
                log.occurredAt(), log.recordedAt());
    }

    public static CursorPage<AuditLogView> toAuditPage(CursorResult<AuditLog> result) {
        return CursorPage.of(result.items().stream().map(ApiViewMapper::toView).toList(), result.size(),
                result.nextCursor());
    }

    public static CursorPage<AccessLogView> toAccessPage(CursorResult<AccessLog> result) {
        return CursorPage.of(result.items().stream().map(ApiViewMapper::toView).toList(), result.size(),
                result.nextCursor());
    }

    public static AuditStatsView toView(StatsResult result) {
        return new AuditStatsView(result.from(), result.to(), result.groupBy(), result.bucket(),
                result.totalEvents(), result.truncated(), result.rows().stream()
                .map(row -> new AuditStatsView.Row(row.dimensions(), row.period(), row.count()))
                .toList());
    }
}
