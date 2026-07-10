package com.apargo.service.auditlog.exception;

import org.springframework.http.HttpStatus;

/**
 * Every error this service can return, in one file, instead of scattering
 * string literals across services/controllers. Add a new failure mode by
 * adding one enum constant here.
 */
public enum AuditErrorCode {

    DUPLICATE_EVENT("AUDIT_4090", "Audit event with this eventId already exists", HttpStatus.CONFLICT),
    VALIDATION_FAILED("AUDIT_4000", "Request failed validation", HttpStatus.BAD_REQUEST),
    NOT_FOUND("AUDIT_4040", "Audit log entry not found", HttpStatus.NOT_FOUND),
    INVALID_FILTER("AUDIT_4001", "Invalid search/filter parameters", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("AUDIT_5000", "Unexpected error processing audit log request", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    AuditErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
