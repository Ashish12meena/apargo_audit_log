package com.apargo.service.auditlog.controller.v1;

import com.apargo.service.auditlog.dto.request.AuditSearchRequest;
import com.apargo.service.auditlog.dto.response.AuditEventResponse;
import com.apargo.service.auditlog.dto.response.PagedResponse;
import com.apargo.service.auditlog.service.query.AuditQueryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/audit-logs
 * <p>
 * Query params (all optional unless noted, all AND'd together):
 * page, limit, search, organizationId, projectId, userId, module, action,
 * entityType, entityId, status, fromDate, toDate, sortBy, sortOrder.
 * <p>
 * Spring binds these directly onto AuditSearchRequest via @ModelAttribute —
 * no manual @RequestParam list to keep in sync.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Audit Log Query", description = "APIs for searching and retrieving audit log events")
public class AuditQueryController {

    private final AuditQueryService queryService;

    @GetMapping
    @Operation(summary = "Search audit logs", description = "Searches audit log events using any combination of filters. All filters are combined with AND. organizationId is mandatory for tenant scoping.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid filter parameters, e.g. organizationId not provided")
    })
    public PagedResponse<AuditEventResponse> search(
            @ParameterObject @Valid AuditSearchRequest filters) {
        return queryService.search(filters);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log event by ID", description = "Fetches a single audit log event by its unique identifier.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit log event fetched successfully"),
            @ApiResponse(responseCode = "404", description = "Audit log event not found")
    })
    public AuditEventResponse getById(
            @Parameter(description = "Audit log event identifier", example = "665f1a2b3c4d5e6f7a8b9c0d", required = true) @PathVariable String id) {
        return queryService.getById(id);
    }
}