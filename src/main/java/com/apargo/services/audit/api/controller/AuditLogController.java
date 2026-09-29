package com.apargo.services.audit.api.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apargo.services.audit.api.context.RequestContext;
import com.apargo.services.audit.api.mapper.ApiViewMapper;
import com.apargo.services.audit.api.request.AuditLogSearchParams;
import com.apargo.services.audit.api.request.AuditStatsParams;
import com.apargo.services.audit.api.response.AuditLogView;
import com.apargo.services.audit.api.response.AuditStatsView;
import com.apargo.services.audit.application.query.AuditLogQueryService;
import com.apargo.services.audit.application.query.AuditStatsService;
import com.apargo.services.audit.common.constant.ApiPaths;
import com.apargo.services.audit.common.response.ApiResponse;
import com.apargo.services.audit.common.response.CursorPage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Tenant audit logs. Scope: {@code X-Org-Id} (required) and {@code X-Project-Id} (optional).
 * Lists use cursor paging, newest first.
 */
@RestController
@RequestMapping(path = ApiPaths.AUDIT_LOGS, produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(prefix = "audit.roles", name = "api", havingValue = "true", matchIfMissing = true)
@Tag(name = "Audit logs")
public class AuditLogController {

    private final AuditLogQueryService queryService;
    private final AuditStatsService statsService;

    public AuditLogController(AuditLogQueryService queryService, AuditStatsService statsService) {
        this.queryService = queryService;
        this.statsService = statsService;
    }

    @GetMapping
    @Operation(summary = "Search audit logs (cursor paging, newest first)")
    public ApiResponse<CursorPage<AuditLogView>> search(RequestContext context,
            @ParameterObject AuditLogSearchParams params) {
        var result = queryService.search(params.toFilter(context), params.cursor(), params.size());
        return ApiResponse.ok(ApiViewMapper.toAuditPage(result), "Audit logs fetched successfully");
    }

    @GetMapping(ApiPaths.STATS)
    @Operation(summary = "Grouped audit counts for a time range")
    public ApiResponse<AuditStatsView> stats(RequestContext context, @ParameterObject AuditStatsParams params) {
        var result = statsService.stats(context.orgId(), context.projectId(), params.from(), params.to(),
                params.groupBy(), params.bucket());
        return ApiResponse.ok(ApiViewMapper.toView(result), "Audit stats fetched successfully");
    }

    @GetMapping(ApiPaths.BY_EVENT_ID)
    @Operation(summary = "Get one audit log")
    public ApiResponse<AuditLogView> get(RequestContext context, @PathVariable String eventId) {
        var log = queryService.get(eventId, context.orgId(), context.projectId());
        return ApiResponse.ok(ApiViewMapper.toView(log), "Audit log fetched successfully");
    }
}
