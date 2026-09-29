package com.apargo.services.audit.application.query;

import java.time.Duration;
import java.util.Objects;

import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.FieldErrorCode;

/** Limits every read query obeys. */
public record QuerySettings(Duration defaultRange, Duration maxRange, int defaultPageSize, int maxPageSize,
                            int maxStatsBuckets) {

    public static final String SIZE_FIELD = "size";

    public QuerySettings {
        Objects.requireNonNull(defaultRange, "defaultRange");
        Objects.requireNonNull(maxRange, "maxRange");
        if (defaultRange.compareTo(maxRange) > 0) {
            throw new IllegalArgumentException("defaultRange must not exceed maxRange");
        }
        if (defaultPageSize < 1 || defaultPageSize > maxPageSize) {
            throw new IllegalArgumentException("defaultPageSize must be between 1 and maxPageSize");
        }
        if (maxStatsBuckets < 1) {
            throw new IllegalArgumentException("maxStatsBuckets must be positive");
        }
    }

    /** Page size from the request: default when absent, 422 when outside 1..maxPageSize. */
    public int resolvePageSize(Integer requested) {
        if (requested == null) {
            return defaultPageSize;
        }
        if (requested < 1 || requested > maxPageSize) {
            throw ApiException.validation(SIZE_FIELD, FieldErrorCode.OUT_OF_RANGE,
                    "size must be between 1 and " + maxPageSize);
        }
        return requested;
    }
}
