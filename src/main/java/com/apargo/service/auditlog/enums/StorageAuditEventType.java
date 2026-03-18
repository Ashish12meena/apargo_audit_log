package com.apargo.service.auditlog.enums;

public enum StorageAuditEventType {

    // ── File lifecycle → storage_file_audit ──────────────────────────────────
    MEDIA_UPLOAD,
    MEDIA_BATCH_UPLOAD,
    MEDIA_DELETE,
    MEDIA_BULK_DELETE,

    // ── Quota → storage_quota_audit ──────────────────────────────────────────
    QUOTA_PROVISIONED,
    QUOTA_UPDATED,
    QUOTA_EXCEEDED,
    QUOTA_RELEASED,

    // ── Access/Abuse → storage_access_audit ──────────────────────────────────
    RATE_LIMIT_EXCEEDED,
    SERVICE_DISABLED
}