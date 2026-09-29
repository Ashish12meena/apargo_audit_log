package com.apargo.services.audit.common.response;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.MDC;

import com.apargo.services.audit.common.constant.LogKeys;
import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code meta} of every response. {@code path} is set on errors only. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMeta(String requestId, String traceId, Instant timestamp, String path) {

    /** Meta for the current request, read from the MDC filled by the request filter. */
    public static ApiMeta current(String path) {
        return new ApiMeta(MDC.get(LogKeys.REQUEST_ID), MDC.get(LogKeys.TRACE_ID),
                Instant.now().truncatedTo(ChronoUnit.MILLIS), path);
    }
}
