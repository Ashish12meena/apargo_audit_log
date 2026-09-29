package com.apargo.services.audit.api.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Stats response {@code data}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditStatsView(
        Instant from,
        Instant to,
        List<String> groupBy,
        String bucket,
        long totalEvents,
        boolean truncated,
        List<Row> results) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Row(Map<String, String> dimensions, String period, long count) {
    }
}
