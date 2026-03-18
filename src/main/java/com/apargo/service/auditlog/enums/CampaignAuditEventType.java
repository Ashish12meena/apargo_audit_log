package com.apargo.service.auditlog.enums;

public enum CampaignAuditEventType {

    // ── Campaign creation ─────────────────────────────────────────────────────
    CAMPAIGN_CREATED,
    CAMPAIGN_SCHEDULED,
    CAMPAIGN_RESCHEDULED,

    // ── Async pipeline ────────────────────────────────────────────────────────
    PREPARATION_STARTED,
    PREPARATION_FAILED,
    DISPATCH_STARTED,

    // ── User-driven lifecycle ─────────────────────────────────────────────────
    CAMPAIGN_PAUSED,
    CAMPAIGN_RESUMED,
    CAMPAIGN_CANCELLED,

    // ── Terminal states ───────────────────────────────────────────────────────
    CAMPAIGN_COMPLETED,
    CAMPAIGN_FAILED
}