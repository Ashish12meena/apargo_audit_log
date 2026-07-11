package com.apargo.service.auditlog.controller.v1;

import com.apargo.service.auditlog.dto.request.AuditStatsRequest;
import com.apargo.service.auditlog.dto.response.AuditStatsResponse;
import com.apargo.service.auditlog.service.query.AuditStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/audit-logs/stats
 * <p>
 * Query params: organizationId (required), projectId, fromDate, toDate,
 * groupBy (comma-separated, 1-2 of: module, eventType, eventStatus,
 * actorType, projectId), bucket (optional: day/hour).
 * <p>
 * Separate controller from {@link AuditQueryController} even though both
 * are read paths — this one returns grouped aggregates, not paged raw
 * documents, and has its own validation rules (mandatory organizationId,
 * whitelisted groupBy), so keeping it distinct avoids overloading one
 * controller with two different response shapes and rule sets.
 */
@RestController
@RequestMapping("/api/v1/audit-logs/stats")
@RequiredArgsConstructor
@Slf4j
public class AuditStatsController {

    private final AuditStatsService statsService;

    @GetMapping
    public AuditStatsResponse getStats(AuditStatsRequest request) {
        log.info("audit_stats_requested orgId={} projectId={} groupBy={} bucket={}",
                request.getOrganizationId(), request.getProjectId(), request.getGroupBy(), request.getBucket());
        return statsService.getStats(request);
    }
}