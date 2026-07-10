package com.apargo.service.auditlog.exception;

/** Thrown when a specific audit log entry is requested by id but doesn't exist. */
public class AuditNotFoundException extends AuditException {

    public AuditNotFoundException(String id) {
        super(AuditErrorCode.NOT_FOUND, "Audit log not found: " + id);
    }
}
