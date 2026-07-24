package com.apargo.service.auditlog.controller.v1;

import com.apargo.service.auditlog.dto.request.AuditSearchRequest;
import com.apargo.service.auditlog.dto.response.AuditEventResponse;
import com.apargo.service.auditlog.dto.response.PagedResponse;
import com.apargo.service.auditlog.service.query.AuditQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/audit-logs
 * <p>
 * Query params (all optional, all AND'd together):
 * page, limit, search, organizationId, projectId, userId, module, action,
 * entityType, entityId, status, fromDate, toDate, sortBy, sortOrder.
 * <p>
 * Spring binds these directly onto AuditSearchRequest via @ModelAttribute —
 * no manual @RequestParam list to keep in sync.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditQueryController {

    private final AuditQueryService queryService;

    @GetMapping
    public PagedResponse<AuditEventResponse> search(@Valid AuditSearchRequest filters) {
        return queryService.search(filters);
    }

    @GetMapping("/{id}")
    public AuditEventResponse getById(@PathVariable String id) {
        return queryService.getById(id);
    }
}
