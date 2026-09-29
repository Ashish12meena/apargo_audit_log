package com.apargo.services.audit.api.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.apargo.services.audit.api.context.RequestContext;
import com.apargo.services.audit.api.mapper.ApiViewMapper;
import com.apargo.services.audit.api.request.AccessLogSearchParams;
import com.apargo.services.audit.api.request.AuditLogSearchParams;
import com.apargo.services.audit.api.response.AccessLogView;
import com.apargo.services.audit.api.response.AuditLogView;
import com.apargo.services.audit.application.query.AccessLogQueryService;
import com.apargo.services.audit.application.query.AuditLogQueryService;
import com.apargo.services.audit.common.constant.ApiPaths;
import com.apargo.services.audit.common.response.ApiResponse;
import com.apargo.services.audit.common.response.CursorPage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Service-to-service reads (support and admin tools), behind {@code X-Internal-Api-Key}.
 * {@code X-Org-Id} is optional here and may be {@code 0} (platform-level data); without it a
 * search must be narrowed by trace id.
 */
@RestController
@ConditionalOnProperty(prefix = "audit.roles", name = "api", havingValue = "true", matchIfMissing = true)
@Tag(name = "Internal")
public class InternalLogController {

    private final AuditLogQueryService auditQueryService;
    private final AccessLogQueryService accessQueryService;

    public InternalLogController(AuditLogQueryService auditQueryService, AccessLogQueryService accessQueryService) {
        this.auditQueryService = auditQueryService;
        this.accessQueryService = accessQueryService;
    }

    @GetMapping(path = ApiPaths.INTERNAL_AUDIT_LOGS, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search audit logs of any organization (X-Org-Id optional, or traceId)")
    public ApiResponse<CursorPage<AuditLogView>> searchAudit(RequestContext context,
            @ParameterObject AuditLogSearchParams params) {
        var result = auditQueryService.search(params.toFilter(context), params.cursor(), params.size());
        return ApiResponse.ok(ApiViewMapper.toAuditPage(result), "Audit logs fetched successfully");
    }

    @GetMapping(path = ApiPaths.INTERNAL_AUDIT_LOGS + ApiPaths.BY_TRACE_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Every audit event of one trace, across services")
    public ApiResponse<CursorPage<AuditLogView>> byTrace(RequestContext context, @PathVariable String traceId,
                                                         @ParameterObject AuditLogSearchParams params) {
        var result = auditQueryService.search(params.toFilter(context, traceId), params.cursor(), params.size());
        return ApiResponse.ok(ApiViewMapper.toAuditPage(result), "Audit logs fetched successfully");
    }

    @GetMapping(path = ApiPaths.INTERNAL_AUDIT_LOGS + ApiPaths.BY_EVENT_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get one audit log of any organization")
    public ApiResponse<AuditLogView> getAudit(RequestContext context, @PathVariable String eventId) {
        var log = auditQueryService.get(eventId, context.orgId(), context.projectId());
        return ApiResponse.ok(ApiViewMapper.toView(log), "Audit log fetched successfully");
    }

    @GetMapping(path = ApiPaths.INTERNAL_ACCESS_LOGS, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search access logs of any organization (X-Org-Id optional, or traceId / actorId)")
    public ApiResponse<CursorPage<AccessLogView>> searchAccess(RequestContext context,
            @ParameterObject AccessLogSearchParams params) {
        var result = accessQueryService.search(params.toFilter(context), params.cursor(), params.size());
        return ApiResponse.ok(ApiViewMapper.toAccessPage(result), "Access logs fetched successfully");
    }

    @GetMapping(path = ApiPaths.INTERNAL_ACCESS_LOGS + ApiPaths.BY_EVENT_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get one access log of any organization")
    public ApiResponse<AccessLogView> getAccess(RequestContext context, @PathVariable String eventId) {
        return ApiResponse.ok(ApiViewMapper.toView(accessQueryService.get(eventId, context.orgId())),
                "Access log fetched successfully");
    }
}
