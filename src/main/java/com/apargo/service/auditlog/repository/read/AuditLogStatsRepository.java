package com.apargo.service.auditlog.repository.read;

import com.apargo.service.auditlog.dto.response.AuditStatsBucket;

import java.time.Instant;
import java.util.List;

/** Aggregation contract for GET /api/v1/audit-logs/stats. */
public interface AuditLogStatsRepository {

    /**
     * @param organizationId mandatory scope — every bucket returned belongs to this org only
     * @param projectId      optional narrower scope within the org
     * @param from           inclusive start of occurredAt range
     * @param to             inclusive end of occurredAt range
     * @param groupByFields  1-2 whitelisted field names (see AuditConstants.GROUPABLE_FIELDS)
     * @param bucket         optional time bucket ("day"/"hour"), null for no time dimension
     */
    List<AuditStatsBucket> aggregate(Long organizationId, Long projectId, Instant from, Instant to,
                                      List<String> groupByFields, String bucket);
}