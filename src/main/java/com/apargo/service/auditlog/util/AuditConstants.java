package com.apargo.service.auditlog.util;

import java.util.Set;

/**
 * Single place for magic strings/numbers used across the audit-log service.
 * If a Mongo field name changes on {@code AuditLog}, update it here — every
 * Criteria-building/query class references these constants, not raw strings.
 */
public final class AuditConstants {

    private AuditConstants() {
    }

    // ── Mongo field names (must match @Field names on AuditLog) ──────────────
    public static final String FIELD_EVENT_ID = "eventId";
    public static final String FIELD_MODULE = "module";
    public static final String FIELD_EVENT_TYPE = "eventType";
    public static final String FIELD_EVENT_STATUS = "eventStatus";
    public static final String FIELD_ORG_ID = "orgId";
    public static final String FIELD_PROJECT_ID = "projectId";
    public static final String FIELD_ACTOR_ID = "actorId";
    public static final String FIELD_ACTOR_TYPE = "actorType";
    public static final String FIELD_ENTITY_TYPE = "entityType";
    public static final String FIELD_ENTITY_ID = "entityId";
    public static final String FIELD_OCCURRED_AT = "occurredAt";
    public static final String FIELD_RECORDED_AT = "recordedAt";
    public static final String FIELD_ERROR_MESSAGE = "errorMessage";
    public static final String FIELD_DEVICE = "device";

    /** Synthetic field, computed at aggregation time only — never stored on AuditLog. */
    public static final String FIELD_BUCKET_DATE = "bucketDate";
    public static final String FIELD_COUNT = "count";

    // ── Pagination defaults ───────────────────────────────────────────────────
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 200;

    // ── Sorting ────────────────────────────────────────────────────────────────
    public static final String DEFAULT_SORT_BY = FIELD_OCCURRED_AT;
    public static final String DEFAULT_SORT_ORDER = "desc";

    /**
     * Whitelist of client-sortable fields. Never pass a raw client-supplied
     * sortBy straight into a Sort/Criteria builder — restrict it to this set
     * first (see AuditPageUtil) to avoid unindexed-field sort performance
     * blowups or field-name injection.
     */
    public static final Set<String> SORTABLE_FIELDS = Set.of(
            FIELD_OCCURRED_AT, FIELD_RECORDED_AT, FIELD_MODULE, FIELD_EVENT_TYPE
    );

    // ── Stats / aggregation ──────────────────────────────────────────────────
    /**
     * Whitelist of fields the stats endpoint may group by. Deliberately
     * restricted to low-cardinality, enum-backed fields — never entityId,
     * actorId, traceId, requestId, or errorMessage, which would fragment
     * into effectively one bucket per document and turn an aggregation
     * into a full-collection dump with extra steps. See AuditStatsService.
     */
    public static final Set<String> GROUPABLE_FIELDS = Set.of(
            FIELD_MODULE, FIELD_EVENT_TYPE, FIELD_EVENT_STATUS, FIELD_ACTOR_TYPE,FIELD_DEVICE, FIELD_PROJECT_ID
    );

    /** Two dimensions covers every real dashboard need we've identified so far. */
    public static final int MAX_GROUP_BY_FIELDS = 2;

    public static final String BUCKET_HOUR = "hour";
    public static final String BUCKET_DAY = "day";
    public static final Set<String> ALLOWED_BUCKETS = Set.of(BUCKET_HOUR, BUCKET_DAY);

    /** Default lookback window when fromDate/toDate aren't supplied on a stats query. */
    public static final int DEFAULT_STATS_LOOKBACK_DAYS = 30;

    // ── Kafka ──────────────────────────────────────────────────────────────────
    public static final String KAFKA_CONSUMER_GROUP = "auditlog-consumer";
}