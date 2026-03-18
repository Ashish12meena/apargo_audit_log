package com.apargo.service.auditlog.model.messaging;

import com.apargo.service.auditlog.enums.AuditActorType;
import com.apargo.service.auditlog.enums.ConversationAuditEventType;
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
@Document(collection = "messaging_conversation_audit")
public class MessagingConversationAuditEvent {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    // ── What happened ─────────────────────────────────────────────────────────

    private ConversationAuditEventType eventType;

    /**
     * Conversation status before the transition.
     * Null for CONVERSATION_CREATED.
     * e.g. "OPEN", "CLOSED", "ARCHIVED"
     */
    private String fromStatus;

    /**
     * Conversation status after the transition.
     * Null for assignment-only events where status didn't change.
     */
    private String toStatus;

    // ── Which conversation ────────────────────────────────────────────────────

    private Long conversationId;
    private Long orgId;
    private Long projectId;
    private Long wabaAccountId;

    /**
     * The contact this conversation belongs to.
     * Useful for "show all audit events for contact X" queries.
     */
    private Long contactId;

    // ── Assignment detail ─────────────────────────────────────────────────────

    /**
     * Present for CONVERSATION_ASSIGNED and CONVERSATION_REASSIGNED.
     * "USER" or "TEAM"
     */
    private String assignedType;

    /**
     * The userId or teamId that the conversation was assigned TO.
     * Present for ASSIGNED and REASSIGNED events.
     */
    private Long assignedId;

    /**
     * The userId or teamId the conversation was assigned FROM.
     * Present for REASSIGNED events only — shows the previous owner.
     */
    private Long previousAssignedId;

    // ── Who triggered it ──────────────────────────────────────────────────────

    /**
     * USER for manual assignment/status changes.
     * SYSTEM for auto-assignment rules or inbound message triggers.
     * WORKER for broadcast-driven conversation creation.
     */
    private AuditActorType actorType;

    /**
     * The userId who performed the action.
     * Null for SYSTEM and WORKER events.
     */
    private Long actorId;

    // ── Timestamps ────────────────────────────────────────────────────────────

    private Instant occurredAt;
    private Instant recordedAt;
}