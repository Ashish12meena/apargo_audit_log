package com.apargo.service.auditlog.model.storage;

import com.apargo.service.auditlog.enums.AuditEventStatus;
import com.apargo.service.auditlog.enums.StorageAuditEventType;
import com.apargo.service.auditlog.enums.QuotaLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "storage_quota_audit")
public class StorageQuotaAuditEvent {

    // ── Identity ─────────────────────────────────────────────────────────────

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("eventId")
    private String eventId;

    // ── Classification ───────────────────────────────────────────────────────

    private StorageAuditEventType eventType;

    private AuditEventStatus eventStatus;

    // ── Who Did It ───────────────────────────────────────────────────────────

    /**
     * Org whose quota was affected. Always present.
     * For QUOTA_EXCEEDED: the org that attempted the upload.
     * For QUOTA_PROVISIONED/UPDATED: the org whose limit was changed.
     */
    private Long orgId;

    /**
     * Project whose quota was affected. Always present.
     * For ORG-level quota events, this will be null
     * since the action targets the org row, not a specific project.
     */
    private Long projectId;

    /**
     * WhatsApp Business Account ID.
     * Present for QUOTA_EXCEEDED — identifies which waba triggered the rejected upload.
     * Null for provisioning, update, and release events.
     */
    private String wabaId;

    // ── Quota Level ──────────────────────────────────────────────────────────

    /**
     * Whether this event is about ORG-level or PROJECT-level quota.
     * Critical for queries like "show all org-level quota changes".
     */
    private QuotaLevel quotaLevel;

    // ── Quota State ──────────────────────────────────────────────────────────

    /**
     * Bytes used at the time of the event.
     * For QUOTA_RELEASED: bytes used BEFORE release (usedBytesAfter = usedBytes - releasedBytes).
     */
    private Long usedBytes;

    /**
     * Maximum allowed bytes at the time of the event.
     * For QUOTA_UPDATED: this is the NEW max (see oldMaxBytes for previous value).
     */
    private Long maxBytes;

    /**
     * Previous max limit before update.
     * Present only for QUOTA_UPDATED. Null for all other event types.
     */
    private Long oldMaxBytes;

    /**
     * How many bytes the upload attempted to consume.
     * Present for QUOTA_EXCEEDED only.
     * usedBytes + requestedBytes > maxBytes explains why it was rejected.
     */
    private Long requestedBytes;

    /**
     * How many bytes were freed by a delete operation.
     * Present for QUOTA_RELEASED only.
     */
    private Long releasedBytes;

    // ── Failure Detail ───────────────────────────────────────────────────────

    /**
     * Present for QUOTA_EXCEEDED with human-readable rejection reason.
     * Null for all other event types.
     */
    private String errorMessage;

    // ── Timestamps ───────────────────────────────────────────────────────────

    private Instant occurredAt;

    private Instant recordedAt;
}