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
@Document(collection = "template_audit")
public class TemplateAuditEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    private com.apargo.service.auditlog.enums.TemplateAuditEventType eventType;

    // ── Routing context (denormalized for query, no joins) ────────────────────
    private Long orgId;
    private Long projectId;
    private String wabaId;
    private Long templateId;

    // ── Status transition (current status in template table is latest only) ───
    private String fromStatus;
    private String toStatus;

    // ── TEMPLATE_CREATED only ─────────────────────────────────────────────────
    // captures intent at creation time — not stored in template table
    private Boolean isDraft;

    // ── TEMPLATE_REJECTED / TEMPLATE_PAUSED only ─────────────────────────────
    // moment-in-time snapshot — can be overwritten in template table later
    private String rejectionReason;

    // ── TEMPLATE_CATEGORY_CHANGED only ───────────────────────────────────────
    // template table only stores current + previous, not the full transition history
    private String previousCategory;
    private String newCategory;

    // ── TEMPLATE_DELETED only ─────────────────────────────────────────────────
    // one-time action flag, not stored in template table
    private Boolean deletedFromMeta;

    // ── TEMPLATE_BULK_DELETED only ────────────────────────────────────────────
    // rows are soft-deleted so count is still queryable, but this is faster
    private Integer deletedCount;

    // ── Actor (not stored anywhere in template service) ───────────────────────
    private AuditActorType actorType;
    private Long actorId;

    // ── Timestamps ────────────────────────────────────────────────────────────
    private Instant occurredAt;
    private Instant recordedAt;
}