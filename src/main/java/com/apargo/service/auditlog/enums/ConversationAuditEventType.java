package com.apargo.service.auditlog.enums;

public enum ConversationAuditEventType {

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    CONVERSATION_CREATED,
    CONVERSATION_CLOSED,
    CONVERSATION_REOPENED,
    CONVERSATION_ARCHIVED,

    // ── Assignment ────────────────────────────────────────────────────────────
    CONVERSATION_ASSIGNED,
    CONVERSATION_REASSIGNED,
    CONVERSATION_UNASSIGNED
}