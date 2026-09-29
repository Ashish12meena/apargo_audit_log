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
import com.apargo.services.audit.api.request.AccessLogSearchParams;
import com.apargo.services.audit.api.response.AccessLogView;
import com.apargo.services.audit.application.query.AccessLogQueryService;
import com.apargo.services.audit.common.constant.ApiPaths;
import com.apargo.services.audit.common.response.ApiResponse;
import com.apargo.services.audit.common.response.CursorPage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Tenant access logs (sign-ins, access decisions). Scope: {@code X-Org-Id}. */
@RestController
@RequestMapping(path = ApiPaths.ACCESS_LOGS, produces = MediaType.APPLICATION_JSON_VALUE)
@ConditionalOnProperty(prefix = "audit.roles", name = "api", havingValue = "true", matchIfMissing = true)
@Tag(name = "Access logs")
public class AccessLogController {

    private final AccessLogQueryService queryService;

    public AccessLogController(AccessLogQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "Search access logs (cursor paging, newest first)")
    public ApiResponse<CursorPage<AccessLogView>> search(RequestContext context,
            @ParameterObject AccessLogSearchParams params) {
        var result = queryService.search(params.toFilter(context), params.cursor(), params.size());
        return ApiResponse.ok(ApiViewMapper.toAccessPage(result), "Access logs fetched successfully");
    }

    @GetMapping(ApiPaths.BY_EVENT_ID)
    @Operation(summary = "Get one access log")
    public ApiResponse<AccessLogView> get(RequestContext context, @PathVariable String eventId) {
        return ApiResponse.ok(ApiViewMapper.toView(queryService.get(eventId, context.orgId())),
                "Access log fetched successfully");
    }
}
