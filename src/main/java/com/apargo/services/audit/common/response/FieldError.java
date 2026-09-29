package com.apargo.services.audit.common.response;

import com.apargo.services.audit.common.error.FieldErrorCode;

/** One invalid field ({@code errors[]} in the response wrapper). */
public record FieldError(String field, String code, String message) {

    public static FieldError of(String field, FieldErrorCode code, String message) {
        return new FieldError(field, code.name(), message);
    }
}
