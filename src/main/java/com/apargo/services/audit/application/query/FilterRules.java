package com.apargo.services.audit.application.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.ErrorCode;
import com.apargo.services.audit.common.error.FieldErrorCode;
import com.apargo.services.audit.common.response.FieldError;

/** Field checks shared by the query services. Each failure is reported against its request field. */
final class FilterRules {

    static final Pattern UPPER_SNAKE = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");
    static final Pattern TRACE_ID = Pattern.compile("[0-9a-f]{32}");
    static final int MAX_ID_LENGTH = 200;

    private final List<FieldError> errors = new ArrayList<>();

    FilterRules upperSnake(String field, String value) {
        if (value != null && !UPPER_SNAKE.matcher(value).matches()) {
            errors.add(FieldError.of(field, FieldErrorCode.INVALID_FORMAT, field + " must be UPPER_SNAKE_CASE"));
        }
        return this;
    }

    FilterRules oneOf(String field, String value, Set<String> allowed) {
        if (value != null && !allowed.contains(value)) {
            errors.add(FieldError.of(field, FieldErrorCode.INVALID_VALUE, field + " must be one of " + allowed));
        }
        return this;
    }

    FilterRules maxLength(String field, String value) {
        if (value != null && value.length() > MAX_ID_LENGTH) {
            errors.add(FieldError.of(field, FieldErrorCode.TOO_LONG, field + " is longer than " + MAX_ID_LENGTH));
        }
        return this;
    }

    FilterRules traceId(String field, String value) {
        if (value != null && !TRACE_ID.matcher(value).matches()) {
            errors.add(FieldError.of(field, FieldErrorCode.INVALID_FORMAT, field + " must be 32 lowercase hex"));
        }
        return this;
    }

    FilterRules requiredWith(String field, String value, String dependentField, String dependentValue) {
        if (dependentValue != null && value == null) {
            errors.add(FieldError.of(field, FieldErrorCode.REQUIRED, field + " is required with " + dependentField));
        }
        return this;
    }

    void throwIfInvalid() {
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
    }

    /**
     * Unscoped (internal) searches must be narrowed by an indexed id, or they would scan every tenant.
     *
     * @param narrowingIds values of the filters that may replace the org scope
     */
    static void requireScope(Long orgId, String message, String... narrowingIds) {
        if (orgId != null) {
            return;
        }
        for (String id : narrowingIds) {
            if (id != null) {
                return;
            }
        }
        throw new ApiException(ErrorCode.BAD_REQUEST, message);
    }
}
