package com.apargo.services.audit.common.constant;

/**
 * Field names and dotted paths of the stored documents. Names equal the platform contract
 * (camelCase); never rename a stored field, only add.
 */
public final class MongoFields {

    public static final String ID = "_id";
    public static final String SCHEMA_VERSION = "schemaVersion";
    public static final String SOURCE_SERVICE = "sourceService";
    public static final String MODULE = "module";
    public static final String EVENT_TYPE = "eventType";
    public static final String STATUS = "status";
    public static final String ORG_ID = "orgId";
    public static final String PROJECT_ID = "projectId";
    public static final String ACTOR = "actor";
    public static final String ENTITY = "entity";
    public static final String CHANGES = "changes";
    public static final String METADATA = "metadata";
    public static final String ERROR = "error";
    public static final String CHANNEL = "channel";
    public static final String REQUEST_ID = "requestId";
    public static final String TRACE_ID = "traceId";
    public static final String IP = "ip";
    public static final String USER_AGENT = "userAgent";
    public static final String OCCURRED_AT = "occurredAt";
    public static final String RECORDED_AT = "recordedAt";
    public static final String ARCHIVE_AT = "archiveAt";

    // access_logs only
    public static final String AUTH_METHOD = "authMethod";
    public static final String FAILURE_CODE = "failureCode";
    public static final String RESOURCE = "resource";
    public static final String COUNTRY = "country";

    // dotted paths used in filters, indexes and aggregations
    public static final String ACTOR_TYPE = "actor.type";
    public static final String ACTOR_ID = "actor.id";
    public static final String ACTOR_EMAIL = "actor.email";
    public static final String ENTITY_TYPE = "entity.type";
    public static final String ENTITY_ID = "entity.id";
    public static final String ERROR_CODE = "error.code";

    /** Keys inside actor / entity / changes[] / error sub-documents. */
    public static final class Nested {
        public static final String TYPE = "type";
        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String EMAIL = "email";
        public static final String IMPERSONATOR_ID = "impersonatorId";
        public static final String FIELD = "field";
        public static final String OLD_VALUE = "oldValue";
        public static final String NEW_VALUE = "newValue";
        public static final String CATEGORY = "category";
        public static final String CODE = "code";
        public static final String MESSAGE = "message";
        public static final String DETAILS = "details";
        public static final String REFERENCE = "reference";

        private Nested() {
        }
    }

    /** archive_manifests fields. */
    public static final class Manifest {
        public static final String COLLECTION = "collection";
        public static final String RUN_ID = "runId";
        public static final String DT = "dt";
        public static final String OBJECT_KEY = "objectKey";
        public static final String DOC_COUNT = "docCount";
        public static final String BYTES = "bytes";
        public static final String SHA256 = "sha256";
        public static final String FIRST_ID = "firstId";
        public static final String LAST_ID = "lastId";
        public static final String MIN_OCCURRED_AT = "minOccurredAt";
        public static final String MAX_OCCURRED_AT = "maxOccurredAt";
        public static final String STATUS = "status";
        public static final String EXPORTED_AT = "exportedAt";
        public static final String DELETED_AT = "deletedAt";
        public static final String ERROR = "error";

        private Manifest() {
        }
    }

    /** job_locks fields. */
    public static final class JobLock {
        public static final String LOCKED_UNTIL = "lockedUntil";
        public static final String LOCKED_AT = "lockedAt";
        public static final String LOCKED_BY = "lockedBy";

        private JobLock() {
        }
    }

    private MongoFields() {
    }
}
