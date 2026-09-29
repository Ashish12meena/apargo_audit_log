package com.apargo.services.audit.common.error;

/**
 * Every error this service returns: HTTP status, API code and default message.
 * Generic codes come from the platform API standard; service codes follow {@code <RESOURCE>_<PROBLEM>}.
 */
public enum ErrorCode {

    BAD_REQUEST(400, "Request could not be read"),
    UNAUTHENTICATED(401, "Authentication is required"),
    FORBIDDEN(403, "Not allowed"),
    NOT_FOUND(404, "Resource not found"),
    METHOD_NOT_ALLOWED(405, "Method not allowed"),
    CONFLICT(409, "Request conflicts with the current state"),
    VALIDATION_FAILED(422, "Request has invalid fields"),
    RATE_LIMITED(429, "Too many requests"),
    INTERNAL_ERROR(500, "Unexpected error"),
    DEPENDENCY_FAILURE(502, "A dependency failed"),
    SERVICE_UNAVAILABLE(503, "Service temporarily unavailable"),
    TIMEOUT(504, "The request took too long"),

    AUDIT_LOG_NOT_FOUND(404, "Audit log not found"),
    ACCESS_LOG_NOT_FOUND(404, "Access log not found");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
