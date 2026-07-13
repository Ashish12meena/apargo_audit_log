package com.apargo.service.auditlog.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Single place every controller's exceptions get mapped to the shared
 * {@link AuditErrorResponse} shape. Previously an empty stub — every
 * {@link AuditException} subclass (DuplicateAuditEventException,
 * AuditNotFoundException, InvalidAuditFilterException, ...) was falling
 * through to Spring Boot's default error body instead of this service's
 * own error contract until this was wired up.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuditException.class)
    public ResponseEntity<AuditErrorResponse> handleAuditException(AuditException ex, HttpServletRequest request) {
        AuditErrorCode errorCode = ex.getErrorCode();

        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("audit_error code={} path={} message={}", errorCode.getCode(), request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("audit_error code={} path={} message={}", errorCode.getCode(), request.getRequestURI(), ex.getMessage());
        }

        return ResponseEntity.status(errorCode.getHttpStatus()).body(toResponse(errorCode, ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuditErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return handleBindingErrors(ex.getBindingResult().getFieldErrors(), request);
    }

    /**
     * Query-param-bound DTOs (implicit {@code @ModelAttribute}, e.g.
     * {@link AuditStatsRequest}) fail validation with {@code BindException},
     * not {@code MethodArgumentNotValidException} — that one's reserved for
     * {@code @RequestBody} DTOs like {@code AuditIngestRequest}. Both need
     * handling or GET-endpoint validation (e.g. missing organizationId on
     * /stats) falls through to the generic 500 handler instead of a clean 400.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<AuditErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        return handleBindingErrors(ex.getFieldErrors(), request);
    }

    private ResponseEntity<AuditErrorResponse> handleBindingErrors(
            java.util.List<org.springframework.validation.FieldError> fieldErrors, HttpServletRequest request) {

        String message = fieldErrors.stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(AuditErrorCode.VALIDATION_FAILED.getDefaultMessage());

        log.warn("audit_validation_failed path={} message={}", request.getRequestURI(), message);

        return ResponseEntity.status(AuditErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(toResponse(AuditErrorCode.VALIDATION_FAILED, message, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuditErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("audit_unexpected_error path={} error={}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(AuditErrorCode.INTERNAL_ERROR.getHttpStatus())
                .body(toResponse(AuditErrorCode.INTERNAL_ERROR, AuditErrorCode.INTERNAL_ERROR.getDefaultMessage(), request));
    }

    private AuditErrorResponse toResponse(AuditErrorCode errorCode, String message, HttpServletRequest request) {
        return AuditErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();
    }
}