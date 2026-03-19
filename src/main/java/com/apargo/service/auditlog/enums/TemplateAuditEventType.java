package com.apargo.service.auditlog.enums;

public enum TemplateAuditEventType {

    // ── CRUD lifecycle ────────────────────────────────────────────────────────
    TEMPLATE_CREATED,       // saved as draft OR submitted directly to Meta
    TEMPLATE_SUBMITTED,     // existing draft pushed to Meta
    TEMPLATE_UPDATED,       // draft components/variables updated
    TEMPLATE_DELETED,       // single soft-delete (with or without Meta deletion)
    TEMPLATE_BULK_DELETED,  // all templates for a project wiped

    // ── Meta status changes (detected during sync) ────────────────────────────
    TEMPLATE_APPROVED,
    TEMPLATE_REJECTED,
    TEMPLATE_PAUSED,
    TEMPLATE_DISABLED,
    TEMPLATE_CATEGORY_CHANGED,

    TEMPLATE_SYNC_STARTED,    // sync job accepted, background worker fired
    TEMPLATE_SYNC_COMPLETED,  // background sync finished with counts
    TEMPLATE_SYNC_FAILED      // background sync threw an error
}
