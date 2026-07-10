package com.apargo.service.auditlog.enums;

/**
 * Single centralized registry of every audit event type across all modules.
 * <p>
 * Previously each module (template, messaging, storage ...) had its own
 * EventType enum tied to its own Mongo collection. Now that every event
 * lands in one {@code audit_log} collection, all event constants live here
 * so there is exactly one place to add/rename/deprecate an event type,
 * and no risk of two modules colliding on the same name.
 * <p>
 * Naming convention: {@code <ENTITY>_<PAST_TENSE_VERB>}.
 * Each block below maps 1:1 to a {@link Module}.
 */
public enum AuditEventType {

    // ── TEMPLATE ────────────────────────────────────────────────────────────
    TEMPLATE_CREATED,
    TEMPLATE_SUBMITTED,
    TEMPLATE_UPDATED,
    TEMPLATE_DELETED,
    TEMPLATE_BULK_DELETED,
    TEMPLATE_APPROVED,
    TEMPLATE_REJECTED,
    TEMPLATE_PAUSED,
    TEMPLATE_DISABLED,
    TEMPLATE_CATEGORY_CHANGED,
    TEMPLATE_SYNC_STARTED,
    TEMPLATE_SYNC_COMPLETED,
    TEMPLATE_SYNC_FAILED,

    // ── MESSAGING_CAMPAIGN ─────────────────────────────────────────────────
    CAMPAIGN_CREATED,
    CAMPAIGN_SCHEDULED,
    CAMPAIGN_RESCHEDULED,
    CAMPAIGN_PREPARATION_STARTED,
    CAMPAIGN_PREPARATION_FAILED,
    CAMPAIGN_DISPATCH_STARTED,
    CAMPAIGN_PAUSED,
    CAMPAIGN_RESUMED,
    CAMPAIGN_CANCELLED,
    CAMPAIGN_COMPLETED,
    CAMPAIGN_FAILED,

    // ── MESSAGING_DISPATCH ─────────────────────────────────────────────────
    DISPATCH_KAFKA_BATCH_PUBLISHED,
    DISPATCH_KAFKA_PUBLISH_FAILED,
    DISPATCH_BATCH_PARTIALLY_FAILED,
    DISPATCH_RECIPIENT_MAX_ATTEMPTS_EXHAUSTED,
    DISPATCH_STALE_LOCK_RECOVERED,

    // ── MESSAGING_CONVERSATION ─────────────────────────────────────────────
    CONVERSATION_CREATED,
    CONVERSATION_CLOSED,
    CONVERSATION_REOPENED,
    CONVERSATION_ARCHIVED,
    CONVERSATION_ASSIGNED,
    CONVERSATION_REASSIGNED,
    CONVERSATION_UNASSIGNED,

    // ── STORAGE ─────────────────────────────────────────────────────────────
    MEDIA_UPLOADED,
    MEDIA_BATCH_UPLOADED,
    MEDIA_DELETED,
    MEDIA_BULK_DELETED,
    QUOTA_PROVISIONED,
    QUOTA_UPDATED,
    QUOTA_EXCEEDED,
    QUOTA_RELEASED,
    STORAGE_RATE_LIMIT_EXCEEDED,
    STORAGE_SERVICE_DISABLED,

    // ── WALLET ──────────────────────────────────────────────────────────────
    WALLET_CREDITED,
    WALLET_DEBITED,
    WALLET_REFUNDED,
    WALLET_LOW_BALANCE,
    WALLET_FROZEN,
    WALLET_UNFROZEN,

    // ── USERS ───────────────────────────────────────────────────────────────
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    USER_LOGIN,
    USER_LOGOUT,
    USER_ROLE_CHANGED,
    USER_PASSWORD_RESET,

    // ── PROJECTS ────────────────────────────────────────────────────────────
    PROJECT_CREATED,
    PROJECT_UPDATED,
    PROJECT_DELETED,
    PROJECT_ARCHIVED,

    // ── PLANS ───────────────────────────────────────────────────────────────
    PLAN_ASSIGNED,
    PLAN_UPGRADED,
    PLAN_DOWNGRADED,
    PLAN_CANCELLED,
    PLAN_RENEWED,

    // ── WABA ────────────────────────────────────────────────────────────────
    WABA_EMBEDDED_SIGNUP_STARTED,
    WABA_EMBEDDED_SIGNUP_COMPLETED,
    WABA_EMBEDDED_SIGNUP_FAILED,
    WABA_DISCONNECTED
}
