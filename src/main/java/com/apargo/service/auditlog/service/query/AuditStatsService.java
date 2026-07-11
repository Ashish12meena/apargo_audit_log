package com.apargo.service.auditlog.service.query;

import com.apargo.service.auditlog.dto.request.AuditStatsRequest;
import com.apargo.service.auditlog.dto.response.AuditStatsBucket;
import com.apargo.service.auditlog.dto.response.AuditStatsResponse;
import com.apargo.service.auditlog.exception.InvalidAuditFilterException;
import com.apargo.service.auditlog.repository.read.AuditLogStatsRepository;
import com.apargo.service.auditlog.util.AuditConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Backs GET /api/v1/audit-logs/stats — grouped counts over a date range,
 * for org-scoped dashboards.
 * <p>
 * Deliberately a direct/live aggregation, not a scheduled rollup: audit
 * volume per org is currently low enough that this is cheap, it's always
 * accurate (no staleness), and it avoids building/maintaining a rollup
 * collection before there's a measured need for one. If dashboard latency
 * or repeated-query cost becomes a real problem, the fix is a precomputed
 * {@code audit_stats_daily} collection for closed days with this service
 * falling back to a live query only for the current, still-accumulating
 * day — that change stays entirely behind this method's signature and
 * the {@link AuditStatsResponse} contract, so callers never notice.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditStatsService {

    private final AuditLogStatsRepository statsRepository;

    public AuditStatsResponse getStats(AuditStatsRequest request) {
        List<String> groupByFields = parseGroupBy(request.getGroupBy());
        validate(request, groupByFields);

        Instant from = request.getFromDate() != null
                ? request.getFromDate()
                : Instant.now().minus(AuditConstants.DEFAULT_STATS_LOOKBACK_DAYS, ChronoUnit.DAYS);
        Instant to = request.getToDate() != null ? request.getToDate() : Instant.now();

        List<AuditStatsBucket> buckets = statsRepository.aggregate(
                request.getOrganizationId(), request.getProjectId(), from, to, groupByFields, request.getBucket());

        long total = buckets.stream().mapToLong(AuditStatsBucket::getCount).sum();

        log.debug("audit_stats orgId={} projectId={} groupBy={} bucket={} resultCount={} totalEvents={}",
                request.getOrganizationId(), request.getProjectId(), groupByFields, request.getBucket(),
                buckets.size(), total);

        return AuditStatsResponse.builder()
                .organizationId(request.getOrganizationId())
                .fromDate(from)
                .toDate(to)
                .groupBy(groupByFields)
                .bucket(request.getBucket())
                .totalEvents(total)
                .results(buckets)
                .build();
    }

    private void validate(AuditStatsRequest request, List<String> groupByFields) {
        if (request.getOrganizationId() == null) {
            // Mandatory, not just an optional filter like on /search — an
            // unscoped aggregation would silently sum every tenant's data
            // into one number instead of returning identifiable rows an
            // operator could notice were wrong.
            throw new InvalidAuditFilterException("organizationId is required for stats queries");
        }

        if (groupByFields.isEmpty()) {
            throw new InvalidAuditFilterException("groupBy must specify at least one field");
        }

        if (groupByFields.size() > AuditConstants.MAX_GROUP_BY_FIELDS) {
            throw new InvalidAuditFilterException(
                    "groupBy supports at most " + AuditConstants.MAX_GROUP_BY_FIELDS + " fields");
        }

        for (String field : groupByFields) {
            if (!AuditConstants.GROUPABLE_FIELDS.contains(field)) {
                throw new InvalidAuditFilterException(
                        "Field '" + field + "' is not groupable. Allowed: " + AuditConstants.GROUPABLE_FIELDS);
            }
        }

        if (request.getBucket() != null && !AuditConstants.ALLOWED_BUCKETS.contains(request.getBucket())) {
            throw new InvalidAuditFilterException(
                    "bucket must be one of " + AuditConstants.ALLOWED_BUCKETS);
        }

        if (request.getFromDate() != null && request.getToDate() != null
                && request.getFromDate().isAfter(request.getToDate())) {
            throw new InvalidAuditFilterException("fromDate must be before toDate");
        }
    }

    private List<String> parseGroupBy(String groupBy) {
        if (groupBy == null || groupBy.isBlank()) {
            return List.of();
        }
        return Arrays.stream(groupBy.split(","))
                .map(String::trim)
                .filter(field -> !field.isEmpty())
                .distinct()
                .toList();
    }
}