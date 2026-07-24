package com.apargo.service.auditlog.controller.v1;

import com.apargo.service.auditlog.dto.request.AuditStatsRequest;
import com.apargo.service.auditlog.dto.response.AuditStatsResponse;
import com.apargo.service.auditlog.service.query.AuditStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
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
@Validated
@Tag(name = "Audit Log Stats", description = "APIs for retrieving grouped/aggregated audit log statistics")
public class AuditStatsController {

    private final AuditStatsService statsService;

    @GetMapping
    @Operation(summary = "Get audit log statistics", description = "Returns grouped audit log aggregates (counts by module, event type, status, actor type, or project) within an optional date range. organizationId is mandatory for tenant scoping.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit log statistics fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Missing organizationId, invalid date range, or unsupported groupBy value")
    })
    public AuditStatsResponse getStats(
            @ParameterObject @Valid AuditStatsRequest request) {
        log.info("audit_stats_requested orgId={} projectId={} groupBy={} bucket={}",
                request.getOrganizationId(), request.getProjectId(), request.getGroupBy(), request.getBucket());
        return statsService.getStats(request);
    }
}