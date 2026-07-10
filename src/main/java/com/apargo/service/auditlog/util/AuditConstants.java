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
    public static final String FIELD_ENTITY_TYPE = "entityType";
    public static final String FIELD_ENTITY_ID = "entityId";
    public static final String FIELD_OCCURRED_AT = "occurredAt";
    public static final String FIELD_RECORDED_AT = "recordedAt";
    public static final String FIELD_ERROR_MESSAGE = "errorMessage";

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

    // ── Kafka ──────────────────────────────────────────────────────────────────
    public static final String KAFKA_CONSUMER_GROUP = "auditlog-consumer";
}
