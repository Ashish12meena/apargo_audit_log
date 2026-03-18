package com.apargo.service.auditlog.model.storage;

import com.apargo.service.auditlog.enums.AuditEventStatus;
import com.apargo.service.auditlog.enums.StorageAuditEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "storage_access_audit")
public class StorageAccessAuditEvent {

    // ── Identity ─────────────────────────────────────────────────────────────

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    // ── Classification ───────────────────────────────────────────────────────

    private StorageAuditEventType eventType;

    /**
     * Always FAILURE for this collection —
     * both RATE_LIMIT_EXCEEDED and SERVICE_DISABLED are rejections.
     * Kept for consistency with other audit collections.
     */
    private AuditEventStatus eventStatus;

    // ── Who Did It ───────────────────────────────────────────────────────────

    /**
     * Org that was rate limited or rejected.
     * Null when request had no valid X-Org-Id header
     * (bad actor, probe, or misconfigured client).
     */
    private Long actorOrgId;

    /**
     * Project that was rate limited or rejected.
     * Null when request had no valid X-Project-Id header.
     */
    private Long actorProjectId;

    /**
     * Client IP address. Always present — extracted from
     * X-Forwarded-For header or remote address as fallback.
     * Primary identifier when org/project context is missing.
     */
    private String actorIp;

    // ── Request Context ──────────────────────────────────────────────────────

    /**
     * The URI that was blocked. e.g. "/api/v1/media/upload"
     * Useful for identifying which endpoints are being abused.
     */
    private String endpoint;

    /**
     * HTTP method of the blocked request. e.g. "POST", "GET"
     */
    private String httpMethod;

    // ── Rate Limit Detail (RATE_LIMIT_EXCEEDED only) ─────────────────────────

    /**
     * Seconds the client was told to wait before retrying.
     * Comes from X-Rate-Limit-Retry-After-Seconds response header.
     * Null for SERVICE_DISABLED events.
     */
    private Long retryAfterSeconds;

    /**
     * The rate limit bucket key that was exhausted.
     * e.g. "org:1:proj:2" or "ip:192.168.1.1"
     * Useful for understanding which bucket triggered the limit.
     */
    private String rateLimitKey;

    // ── Failure Detail ───────────────────────────────────────────────────────

    private String errorMessage;

    // ── Timestamps ───────────────────────────────────────────────────────────

   
    private Instant occurredAt;

    private Instant recordedAt;
}