package com.apargo.services.audit.application.query;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.apargo.services.audit.application.port.out.AuditLogReader;
import com.apargo.services.audit.common.enums.StatsDimension;
import com.apargo.services.audit.common.enums.TimeBucket;
import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.FieldErrorCode;
import com.apargo.services.audit.common.response.FieldError;

/** Grouped audit counts for dashboards, always scoped to one organization and a bounded range. */
public final class AuditStatsService {

    public static final int MAX_GROUP_BY = 2;
    private static final String GROUP_BY_FIELD = "groupBy";
    private static final String BUCKET_FIELD = "bucket";

    private final AuditLogReader reader;
    private final QuerySettings settings;
    private final Clock clock;

    public AuditStatsService(AuditLogReader reader, QuerySettings settings, Clock clock) {
        this.reader = Objects.requireNonNull(reader);
        this.settings = Objects.requireNonNull(settings);
        this.clock = Objects.requireNonNull(clock);
    }

    public StatsResult stats(long orgId, Long projectId, Instant from, Instant to, String groupBy, String bucket) {
        List<FieldError> errors = new ArrayList<>();
        List<StatsDimension> dimensions = parseGroupBy(groupBy, errors);
        TimeBucket timeBucket = parseBucket(bucket, errors);
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        TimeRange range = TimeRange.resolve(from, to, clock.instant(), settings);

        StatsQuery query = new StatsQuery(orgId, projectId, range.from(), range.to(), dimensions, timeBucket);
        List<StatsRow> rows = reader.stats(query, settings.maxStatsBuckets() + 1);
        boolean truncated = rows.size() > settings.maxStatsBuckets();
        if (truncated) {
            rows = rows.subList(0, settings.maxStatsBuckets());
        }
        long total = rows.stream().mapToLong(StatsRow::count).sum();
        return new StatsResult(range.from(), range.to(), dimensions.stream().map(StatsDimension::apiName).toList(),
                timeBucket == null ? null : timeBucket.apiName(), total, truncated, rows);
    }

    private static List<StatsDimension> parseGroupBy(String groupBy, List<FieldError> errors) {
        Set<String> names = new LinkedHashSet<>();
        if (groupBy != null) {
            Arrays.stream(groupBy.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(names::add);
        }
        if (names.isEmpty()) {
            errors.add(FieldError.of(GROUP_BY_FIELD, FieldErrorCode.REQUIRED, "groupBy is required"));
            return List.of();
        }
        List<StatsDimension> dimensions = new ArrayList<>();
        for (String name : names) {
            StatsDimension.fromApiName(name).ifPresentOrElse(dimensions::add, () -> errors.add(
                    FieldError.of(GROUP_BY_FIELD, FieldErrorCode.INVALID_VALUE, "'" + name + "' is not groupable; allowed: "
                            + Arrays.stream(StatsDimension.values()).map(StatsDimension::apiName).toList())));
        }
        if (dimensions.size() > MAX_GROUP_BY) {
            errors.add(FieldError.of(GROUP_BY_FIELD, FieldErrorCode.OUT_OF_RANGE,
                    "groupBy supports at most " + MAX_GROUP_BY + " fields"));
        }
        return dimensions;
    }

    private static TimeBucket parseBucket(String bucket, List<FieldError> errors) {
        if (bucket == null || bucket.isBlank()) {
            return null;
        }
        return TimeBucket.fromApiName(bucket.trim()).orElseGet(() -> {
            errors.add(FieldError.of(BUCKET_FIELD, FieldErrorCode.INVALID_VALUE, "bucket must be one of "
                    + Arrays.stream(TimeBucket.values()).map(TimeBucket::apiName).toList()));
            return null;
        });
    }
}
