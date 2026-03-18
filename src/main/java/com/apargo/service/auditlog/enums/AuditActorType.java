package com.apargo.service.auditlog.enums;

/**
 * Who triggered the audit event.
 * USER   → API call from a real user
 * SYSTEM → internal service action (no human involved)
 * SCHEDULER → triggered by a scheduled job
 * WORKER → triggered by an async worker thread (preparation, dispatch)
 */
public enum AuditActorType {
    USER,
    SYSTEM,
    SCHEDULER,
    WORKER
}