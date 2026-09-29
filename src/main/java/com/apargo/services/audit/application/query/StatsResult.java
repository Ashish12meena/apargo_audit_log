package com.apargo.services.audit.application.query;

import java.time.Instant;
import java.util.List;

/** Stats response content. {@code truncated} means more groups existed than the bucket limit. */
public record StatsResult(Instant from, Instant to, List<String> groupBy, String bucket, long totalEvents,
                          boolean truncated, List<StatsRow> rows) {

    public StatsResult {
        groupBy = List.copyOf(groupBy);
        rows = List.copyOf(rows);
    }
}
