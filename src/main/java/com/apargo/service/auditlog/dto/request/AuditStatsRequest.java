package com.apargo.service.auditlog.dto.request;

import lombok.Data;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

/**
 * Backing object for GET /api/v1/audit-logs/stats, populated via
 * {@code @ModelAttribute} — same binding style as {@link AuditSearchRequest}.
 * <p>
 * Unlike search, {@code organizationId} is mandatory here: an aggregation
 * with no org scope would silently sum counts across every tenant into one
 * number, which is worse than an unscoped search (that at least still
 * returns identifiable per-row data an operator could notice is wrong).
 * Enforcement happens in {@code AuditStatsService.validate}, not via bean
 * validation, so the 400 response can explain *why* in the same
 * {@code AuditErrorResponse} shape every other error in this service uses.
 * <p>
 * {@code groupBy} is a comma-separated list of field names (e.g.
 * "module,eventStatus") rather than a repeated query param, to keep the
 * URL simple for dashboard callers: {@code ?groupBy=module,eventStatus}.
 */
@Data
public class AuditStatsRequest {

    @NotNull(message = "organizationId is required for stats queries")
    private Long organizationId;
    private Long projectId;

    private Instant fromDate;
    private Instant toDate;

    /** Comma-separated field names, e.g. "module,eventStatus". Parsed by AuditStatsService. */
    private String groupBy;

    /** Optional time bucket: "day" or "hour". Null means no time dimension. */
    private String bucket;
}