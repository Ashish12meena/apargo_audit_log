package com.apargo.services.audit.application.query;

import java.time.Instant;
import java.util.List;

import com.apargo.services.audit.common.enums.StatsDimension;
import com.apargo.services.audit.common.enums.TimeBucket;

/** A validated stats request. {@code bucket} is {@code null} when no time dimension is wanted. */
public record StatsQuery(long orgId, Long projectId, Instant from, Instant to, List<StatsDimension> groupBy,
                         TimeBucket bucket) {

    public StatsQuery {
        groupBy = List.copyOf(groupBy);
    }
}
