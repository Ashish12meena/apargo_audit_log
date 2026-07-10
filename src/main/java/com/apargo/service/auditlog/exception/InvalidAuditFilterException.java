package com.apargo.service.auditlog.exception;

/** Thrown when GET /api/v1/audit-logs query params are malformed/contradictory. */
public class InvalidAuditFilterException extends AuditException {

    public InvalidAuditFilterException(String message) {
        super(AuditErrorCode.INVALID_FILTER, message);
    }
}
