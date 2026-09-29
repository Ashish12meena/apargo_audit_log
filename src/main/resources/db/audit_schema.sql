-- ============================================================================================
-- Audit service schema: relational reference (MySQL 8.0+)
--
-- The service stores its data in MongoDB. The runtime definitions (validators and indexes) are
-- db/mongo/*.json, applied by MongoSchemaInitializer or scripts/apply-mongo-schema.js. This file
-- describes the same collections as tables, for documentation, reporting exports and analysis.
--
-- Mapping rules
--   * Column names are the stored field names (platform contract, camelCase).
--   * Nested MongoDB fields are flattened: actor.id -> actorId, entity.type -> entityType,
--     error.code -> errorCode. Free-form parts (changes, metadata, error.details) are JSON.
--   * MongoDB _id is the event id (UUIDv7) -> column eventId.
--   * Times are UTC. archiveAt = recordedAt + hot days; nothing is deleted by TTL.
--   * MongoDB partial indexes have no MySQL equivalent; they are noted where they apply.
-- ============================================================================================


-- --------------------------------------------------------------------------------------------
-- 1. audit_logs: business changes and business failures (AuditEventDto)
-- --------------------------------------------------------------------------------------------
CREATE TABLE audit_logs (
    eventId             CHAR(36)      NOT NULL COMMENT 'UUIDv7 from the producer; MongoDB _id',
    schemaVersion       INT           NOT NULL DEFAULT 1,
    sourceService       VARCHAR(100)  NOT NULL COMMENT 'e.g. template-service',
    module              VARCHAR(50)   NOT NULL COMMENT 'UPPER_SNAKE_CASE, e.g. TEMPLATE',
    eventType           VARCHAR(100)  NOT NULL COMMENT '<ENTITY>_<PAST_VERB>, e.g. TEMPLATE_UPDATED',
    status              VARCHAR(20)   NOT NULL COMMENT 'SUCCESS | FAILURE',

    orgId               BIGINT        NOT NULL COMMENT '0 = platform-level data',
    projectId           BIGINT        NULL,

    actorType           VARCHAR(30)   NOT NULL COMMENT 'actor.type: USER | SERVICE | SYSTEM',
    actorId             VARCHAR(100)  NULL     COMMENT 'actor.id: user id, service name or job name',
    actorName           VARCHAR(255)  NULL     COMMENT 'actor.name',
    actorImpersonatorId VARCHAR(100)  NULL     COMMENT 'actor.impersonatorId',

    entityType          VARCHAR(50)   NULL     COMMENT 'entity.type; absent for bulk events',
    entityId            VARCHAR(100)  NULL     COMMENT 'entity.id; absent only on FAILURE before creation',
    entityName          VARCHAR(255)  NULL     COMMENT 'entity.name',

    changes             JSON          NOT NULL COMMENT '[{field, oldValue, newValue}], [] when none, max 100',
    metadata            JSON          NOT NULL COMMENT 'module-specific keys as sent, {} when none',

    errorCategory       VARCHAR(30)   NULL     COMMENT 'error.category: VALIDATION | BUSINESS | AUTHENTICATION | AUTHORIZATION | EXTERNAL_SERVICE | SYSTEM',
    errorCode           VARCHAR(100)  NULL     COMMENT 'error.code: the service error code',
    errorMessage        VARCHAR(500)  NULL     COMMENT 'error.message: user-safe text',
    errorDetails        JSON          NULL     COMMENT 'error.details',
    errorReference      VARCHAR(100)  NULL     COMMENT 'error.reference: the trace id',

    channel             VARCHAR(20)   NULL     COMMENT 'WEB | API | MOBILE | WORKER',
    requestId           VARCHAR(100)  NULL     COMMENT 'X-Request-Id; absent for scheduled jobs',
    traceId             CHAR(32)      NULL     COMMENT 'W3C trace id, 32 lowercase hex',
    ip                  VARCHAR(45)   NULL,
    userAgent           VARCHAR(500)  NULL,

    occurredAt          DATETIME(3)   NOT NULL COMMENT 'when it happened (producer clock)',
    recordedAt          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'when the audit service stored it',
    archiveAt           DATETIME(3)   NOT NULL COMMENT 'recordedAt + hot days (default 30)',

    PRIMARY KEY (eventId),
    KEY org_time          (orgId, occurredAt DESC, eventId DESC),
    KEY org_project_time  (orgId, projectId, occurredAt DESC, eventId DESC)            COMMENT 'Mongo: partial, projectId exists',
    KEY org_entity_time   (orgId, entityType, entityId, occurredAt DESC, eventId DESC) COMMENT 'Mongo: partial, entity.id exists',
    KEY org_actor_time    (orgId, actorId, occurredAt DESC, eventId DESC),
    KEY org_module_time   (orgId, module, occurredAt DESC, eventId DESC),
    KEY org_failures_time (orgId, status, occurredAt DESC, eventId DESC)               COMMENT 'Mongo: partial, status = FAILURE',
    KEY trace             (traceId),
    KEY archive_scan      (archiveAt, eventId),

    CONSTRAINT chk_audit_status CHECK (status IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_audit_org    CHECK (orgId >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'Business history: who changed what, in every service';


-- --------------------------------------------------------------------------------------------
-- 2. access_logs: sign-ins, sign-outs and access decisions
--    Every AccessEventDto field plus the optional schema-doc fields (authMethod, resource,
--    country, metadata, actorEmail, actorImpersonatorId), stored when a producer sends them.
-- --------------------------------------------------------------------------------------------
CREATE TABLE access_logs (
    eventId             CHAR(36)      NOT NULL COMMENT 'UUIDv7; MongoDB _id',
    schemaVersion       INT           NOT NULL DEFAULT 1,
    sourceService       VARCHAR(100)  NOT NULL COMMENT 'auth-service or gateway',
    eventType           VARCHAR(100)  NOT NULL COMMENT 'LOGIN_SUCCEEDED | LOGIN_FAILED | TOKEN_REFRESHED | LOGGED_OUT | ACCESS_DENIED | ...',
    status              VARCHAR(20)   NOT NULL COMMENT 'SUCCESS | FAILURE | DENIED',

    orgId               BIGINT        NULL     COMMENT 'null when the principal was not identified',

    actorType           VARCHAR(30)   NOT NULL COMMENT 'actor.type: USER | SERVICE | SYSTEM | API_KEY | SUPPORT | UNKNOWN',
    actorId             VARCHAR(100)  NULL     COMMENT 'actor.id; null when sign-in failed before identification',
    actorName           VARCHAR(255)  NULL     COMMENT 'actor.name',
    actorEmail          VARCHAR(255)  NULL     COMMENT 'actor.email, masked (ra***@acme.com)',
    actorImpersonatorId VARCHAR(100)  NULL     COMMENT 'actor.impersonatorId',

    authMethod          VARCHAR(30)   NULL     COMMENT 'PASSWORD | OTP | GOOGLE | API_KEY',
    failureCode         VARCHAR(100)  NULL     COMMENT 'e.g. INVALID_CREDENTIALS (also accepted as failureReason)',
    resource            VARCHAR(500)  NULL     COMMENT 'e.g. POST /v1/campaigns, for denied access',

    ip                  VARCHAR(45)   NULL,
    userAgent           VARCHAR(500)  NULL,
    country             VARCHAR(10)   NULL     COMMENT 'from IP',
    metadata            JSON          NULL,

    requestId           VARCHAR(100)  NULL,
    traceId             CHAR(32)      NULL,

    occurredAt          DATETIME(3)   NOT NULL,
    recordedAt          DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    archiveAt           DATETIME(3)   NOT NULL,

    PRIMARY KEY (eventId),
    KEY org_time                (orgId, occurredAt DESC, eventId DESC),
    KEY actor_time              (actorId, occurredAt DESC, eventId DESC)  COMMENT 'Mongo: partial, actor.id exists',
    KEY org_type_time           (orgId, eventType, occurredAt DESC, eventId DESC),
    KEY actor_email_status_time (actorEmail, status, occurredAt DESC)     COMMENT 'Mongo: partial, actor.email exists',
    KEY trace                   (traceId)                                 COMMENT 'Mongo: partial, traceId exists',
    KEY archive_scan            (archiveAt, eventId),

    CONSTRAINT chk_access_org CHECK (orgId IS NULL OR orgId >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'Authentication and security history';


-- --------------------------------------------------------------------------------------------
-- 3. archive_manifests: one row per exported cold-storage file
-- --------------------------------------------------------------------------------------------
CREATE TABLE archive_manifests (
    id                  VARCHAR(200)  NOT NULL COMMENT '<collection>/<dt>/<runId>-<part>; MongoDB _id',
    collection          VARCHAR(50)   NOT NULL COMMENT 'audit_logs | access_logs',
    runId               VARCHAR(30)   NOT NULL COMMENT 'e.g. 20261030T021500Z',
    dt                  CHAR(10)      NOT NULL COMMENT 'UTC day of occurredAt, YYYY-MM-DD',
    objectKey           VARCHAR(500)  NOT NULL COMMENT 'path of the file in the archive store',
    docCount            BIGINT        NOT NULL,
    bytes               BIGINT        NULL,
    sha256              CHAR(64)      NULL,
    firstId             CHAR(36)      NULL,
    lastId              CHAR(36)      NULL,
    minOccurredAt       DATETIME(3)   NULL,
    maxOccurredAt       DATETIME(3)   NULL,
    status              VARCHAR(20)   NOT NULL COMMENT 'EXPORTED | DELETED | FAILED',
    exportedAt          DATETIME(3)   NOT NULL,
    deletedAt           DATETIME(3)   NULL,
    error               VARCHAR(1000) NULL,

    PRIMARY KEY (id),
    KEY collection_dt   (collection, dt),
    KEY status_exported (status, exportedAt),

    CONSTRAINT chk_manifest_status CHECK (status IN ('EXPORTED', 'DELETED', 'FAILED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'Proof of every archive export; documents are deleted only after a verified export';


-- --------------------------------------------------------------------------------------------
-- 4. job_locks: lease lock so only one instance runs a scheduled job
-- --------------------------------------------------------------------------------------------
CREATE TABLE job_locks (
    id                  VARCHAR(100)  NOT NULL COMMENT 'job name, e.g. audit-archiver; MongoDB _id',
    lockedUntil         DATETIME(3)   NOT NULL,
    lockedAt            DATETIME(3)   NOT NULL,
    lockedBy            VARCHAR(255)  NOT NULL COMMENT 'instance id holding the lease',

    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = 'Scheduler lease locks';
