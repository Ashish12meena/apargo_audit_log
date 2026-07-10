package com.apargo.service.auditlog.exception;

/** Thrown when an eventId already exists — Kafka redelivery or REST retry. */
public class DuplicateAuditEventException extends AuditException {

    public DuplicateAuditEventException(String eventId) {
        super(AuditErrorCode.DUPLICATE_EVENT, "Duplicate audit event: " + eventId);
    }
}
