package com.apargo.service.auditlog.model.template;

import com.apargo.service.auditlog.enums.AuditActorType;
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
@Document(collection = "template_sync_audit")
public class TemplateSyncAuditEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    private TemplateAuditEvent eventType;

    // ── Routing context ───────────────────────────────────────────────────────
    private Long orgId;
    private Long projectId;
    private String wabaId;

    // ── TEMPLATE_SYNC_COMPLETED (not stored anywhere in template service) ─────
    private Integer insertedCount;
    private Integer updatedCount;
    private Integer deletedCount;
    private Integer skippedCount;
    private Long durationMs;

    // ── TEMPLATE_SYNC_FAILED (not stored anywhere in template service) ────────
    private String failureReason;

    // ── Actor ─────────────────────────────────────────────────────────────────
    private AuditActorType actorType;
    private Long actorId;

    // ── Timestamps ────────────────────────────────────────────────────────────
    private Instant occurredAt;
    private Instant recordedAt;
}