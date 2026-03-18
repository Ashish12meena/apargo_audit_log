package com.apargo.service.auditlog.model.messaging;

import com.apargo.service.auditlog.enums.DispatchAuditEventType;
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
@Document(collection = "messaging_dispatch_audit")
public class MessagingDispatchAuditEvent {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    // ── What happened ─────────────────────────────────────────────────────────

    private DispatchAuditEventType eventType;

    // ── Which campaign ────────────────────────────────────────────────────────

    private Long campaignId;
    private Long orgId;
    private Long projectId;
    private Long wabaAccountId;

    // ── Batch detail (KAFKA_BATCH_PUBLISHED, BATCH_PARTIALLY_FAILED) ──────────

    /**
     * How many recipients were in this dispatch batch.
     */
    private Integer batchSize;

    /**
     * How many were successfully published to Kafka.
     * Null for non-batch events.
     */
    private Integer successCount;

    /**
     * How many failed in this batch.
     * For KAFKA_PUBLISH_FAILED this equals batchSize (entire batch failed).
     * For BATCH_PARTIALLY_FAILED this is less than batchSize.
     */
    private Integer failureCount;

    // ── Recovery detail (STALE_LOCK_RECOVERED) ───────────────────────────────

    /**
     * How many locked recipients were recovered back to PENDING.
     * Present only for STALE_LOCK_RECOVERED events.
     */
    private Integer recoveredCount;

    // ── Worker context ────────────────────────────────────────────────────────

    /**
     * The dispatch worker instance that processed this batch.
     * e.g. "dispatch-a1b2c3d4". Useful for debugging which pod was involved.
     */
    private String workerName;

    /**
     * The Kafka topic published to.
     * e.g. "whatsapp.messages.outbound"
     * Null for non-Kafka events.
     */
    private String kafkaTopic;

    // ── Failure detail ────────────────────────────────────────────────────────

    /**
     * Error message for KAFKA_PUBLISH_FAILED and PREPARATION_FAILED events.
     */
    private String errorMessage;

    // ── Timestamps ────────────────────────────────────────────────────────────

    private Instant occurredAt;
    private Instant recordedAt;
}