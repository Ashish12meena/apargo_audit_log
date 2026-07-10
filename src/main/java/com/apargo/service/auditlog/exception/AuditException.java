package com.apargo.service.auditlog.exception;

/**
 * Base unchecked exception for every failure in this service. Always carries
 * an AuditErrorCode so the GlobalAuditExceptionHandler can map it to the
 * correct HTTP status without a big if/else chain.
 */
public class AuditException extends RuntimeException {

    private final AuditErrorCode errorCode;

    public AuditException(AuditErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public AuditException(AuditErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuditException(AuditErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public AuditErrorCode getErrorCode() {
        return errorCode;
    }
}
