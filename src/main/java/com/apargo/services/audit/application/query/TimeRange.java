package com.apargo.services.audit.application.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.FieldErrorCode;
import com.apargo.services.audit.common.response.FieldError;

/**
 * The {@code [from, to)} window of a query. Every read is time-bounded so it stays on an index
 * and inside the hot data.
 */
public record TimeRange(Instant from, Instant to) {

    public static final String FROM_FIELD = "from";
    public static final String TO_FIELD = "to";

    /** Defaults: {@code to} = now, {@code from} = {@code to} − default range. */
    public static TimeRange resolve(Instant from, Instant to, Instant now, QuerySettings settings) {
        Instant end = to != null ? to : now;
        Instant start = from != null ? from : end.minus(settings.defaultRange());

        List<FieldError> errors = new ArrayList<>();
        if (!start.isBefore(end)) {
            errors.add(FieldError.of(FROM_FIELD, FieldErrorCode.INVALID_VALUE, "from must be before to"));
        } else if (start.plus(settings.maxRange()).isBefore(end)) {
            errors.add(FieldError.of(TO_FIELD, FieldErrorCode.OUT_OF_RANGE,
                    "the range must not exceed " + settings.maxRange().toDays() + " days"));
        }
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        return new TimeRange(start, end);
    }
}
