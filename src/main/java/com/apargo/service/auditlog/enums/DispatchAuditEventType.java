package com.apargo.service.auditlog.enums;

public enum DispatchAuditEventType {

    // ── Kafka publish ─────────────────────────────────────────────────────────
    KAFKA_BATCH_PUBLISHED,
    KAFKA_PUBLISH_FAILED,

    // ── Recipient outcomes ────────────────────────────────────────────────────
    BATCH_PARTIALLY_FAILED,
    RECIPIENT_MAX_ATTEMPTS_EXHAUSTED,

    // ── Housekeeping ──────────────────────────────────────────────────────────
    STALE_LOCK_RECOVERED
}