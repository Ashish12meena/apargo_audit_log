package com.apargo.services.audit.common.error;

/** Field error codes from the platform API standard ({@code errors[].code}). */
public enum FieldErrorCode {
    REQUIRED,
    INVALID_FORMAT,
    INVALID_VALUE,
    TOO_LONG,
    OUT_OF_RANGE,
    DUPLICATE,
    UNKNOWN_FIELD
}
