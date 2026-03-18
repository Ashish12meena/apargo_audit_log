package com.apargo.service.auditlog.model.messaging;

import com.apargo.service.auditlog.enums.AuditActorType;
import com.apargo.service.auditlog.enums.CampaignAuditEventType;
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
@Document(collection = "messaging_campaign_audit")
public class MessagingCampaignAuditEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    // ── What happened ─────────────────────────────────────────────────────────

    private CampaignAuditEventType eventType;
    private String fromStatus;
    private String toStatus;

    // ── Which campaign ────────────────────────────────────────────────────────

    private Long campaignId;
    private Long orgId;
    private Long projectId;
    private Long wabaAccountId;

    // ── Who triggered it ──────────────────────────────────────────────────────

    private AuditActorType actorType;
    private Long actorId;

    // ── CAMPAIGN_CREATED ──────────────────────────────────────────────────────

    private Long templateId;
    private Integer totalPhoneNumbers;
    private Integer mediaCount;

    // ── CAMPAIGN_SCHEDULED / CAMPAIGN_RESCHEDULED ─────────────────────────────

    private Instant scheduledAt;
    private Instant previousScheduledAt;
    private Boolean isReschedule;

    // ── CAMPAIGN_COMPLETED ────────────────────────────────────────────────────

    private Long sentCount;
    private Long failedCount;
    private Long skippedCount;

    // ── CAMPAIGN_FAILED / PREPARATION_FAILED ──────────────────────────────────

    private String failureStage;
    private String failureReason;

    // ── DISPATCH_STARTED ──────────────────────────────────────────────────────

    private Boolean isRecovery;

    // ── CAMPAIGN_CANCELLED ────────────────────────────────────────────────────

    private String cancelledAtStage;

    // ── Timestamps ────────────────────────────────────────────────────────────

    private Instant occurredAt;
    private Instant recordedAt;
}