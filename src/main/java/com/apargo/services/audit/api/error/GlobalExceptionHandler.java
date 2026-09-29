package com.apargo.services.audit.api.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.apargo.services.audit.application.port.out.QueryTimeoutException;
import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.apargo.services.audit.common.constant.ApiHeaders;
import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.ErrorCode;
import com.apargo.services.audit.common.error.FieldErrorCode;
import com.apargo.services.audit.common.response.ApiResponse;
import com.apargo.services.audit.common.response.FieldError;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Turns every exception into the standard error wrapper. Messages are user-safe: stack traces,
 * driver messages and internal details go to the log only.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Seconds a client should wait before retrying a 503. */
    private static final String RETRY_AFTER_SECONDS = "5";
    private static final String TYPE_MISMATCH = "typeMismatch";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException e, HttpServletRequest request) {
        log.debug("api_error code={} path={} message={}", e.errorCode(), request.getRequestURI(), e.getMessage());
        return respond(e.errorCode(), e.getMessage(), e.fieldErrors(), request);
    }

    /** Query parameters bound to a request object (type mismatch, e.g. an invalid date). */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBind(BindException e, HttpServletRequest request) {
        List<FieldError> errors = e.getFieldErrors().stream()
                .map(error -> FieldError.of(error.getField(),
                        TYPE_MISMATCH.equals(error.getCode()) ? FieldErrorCode.INVALID_FORMAT : FieldErrorCode.INVALID_VALUE,
                        error.getField() + " is not valid"))
                .toList();
        return respond(ErrorCode.VALIDATION_FAILED, null, errors, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                                HttpServletRequest request) {
        return respond(ErrorCode.VALIDATION_FAILED, null,
                List.of(FieldError.of(e.getName(), FieldErrorCode.INVALID_FORMAT, e.getName() + " is not valid")),
                request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e,
                                                                    HttpServletRequest request) {
        return respond(ErrorCode.VALIDATION_FAILED, null, List.of(FieldError.of(e.getParameterName(),
                FieldErrorCode.REQUIRED, e.getParameterName() + " is required")), request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException e,
                                                                 HttpServletRequest request) {
        return respond(ErrorCode.BAD_REQUEST, e.getHeaderName() + " header is required", List.of(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e,
                                                              HttpServletRequest request) {
        return respond(ErrorCode.BAD_REQUEST, null, List.of(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        return respond(ErrorCode.NOT_FOUND, null, List.of(), request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException e,
                                                          HttpServletRequest request) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, null, List.of(), request);
    }

    /** The client does not accept JSON, so no wrapper can be written: status only. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleTimeout(QueryTimeoutException e, HttpServletRequest request) {
        log.warn("api_query_timeout path={} cause={}", request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.TIMEOUT, "The query took too long; narrow the time range or add filters",
                List.of(), request);
    }

    @ExceptionHandler(TransientFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnavailable(TransientFailureException e,
                                                               HttpServletRequest request) {
        log.error("api_dependency_unavailable path={} cause={}", request.getRequestURI(), e.getMessage());
        return respond(ErrorCode.SERVICE_UNAVAILABLE, null, List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("api_unexpected_error path={}", request.getRequestURI(), e);
        return respond(ErrorCode.INTERNAL_ERROR, null, List.of(), request);
    }

    private static ResponseEntity<ApiResponse<Void>> respond(ErrorCode code, String message, List<FieldError> errors,
                                                             HttpServletRequest request) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(code.httpStatus());
        if (code == ErrorCode.SERVICE_UNAVAILABLE || code == ErrorCode.RATE_LIMITED) {
            builder.header(ApiHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS);
        }
        builder.header(HttpHeaders.CACHE_CONTROL, "no-store");
        return builder.body(ApiResponse.error(code, message, errors, request.getRequestURI()));
    }
}
