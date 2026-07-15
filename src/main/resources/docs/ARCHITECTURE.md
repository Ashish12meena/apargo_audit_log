# Architecture — Audit Log Service (`auditlog`)

## 1. Tech Stack

| Layer | Choice |
|---|---|
| Language / Framework | Java, Spring Boot |
| Database | MongoDB (schema-less — every module's metadata shape differs, so a fixed-column relational table would need a migration per new event type) |
| Messaging | Apache Kafka (Spring Kafka) — async, high-volume ingestion path |
| Service discovery | Netflix Eureka |
| API docs | springdoc-openapi (Swagger UI at `/swagger-ui.html`, raw spec at `/v3/api-docs`) |
| Validation | Jakarta Bean Validation (`@Valid`) |
| Boilerplate reduction | Lombok (`@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`) |
| Deployment | VPS, systemd-managed service (not containerized/K8s at this stage) |

## 2. High-Level Flow

```
                 ┌──────────────────────┐
producing        │  REST: POST          │
services  ─────► │  /api/v1/audit-logs  │──┐
(template,        └──────────────────────┘  │
 messaging,                                  ▼
 storage,        ┌──────────────────────┐  ┌────────────────────┐
 wallet, ...)     │  Kafka: audit.events │─►│ AuditCommandService │──► MongoDB (audit_log)
                 │  (+ legacy per-module │  │  (single write path)│      write template
                 │   topics)             │  └────────────────────┘      (primary node)
                 └──────────────────────┘

                                     ┌────────────────────────┐
support / dashboards ── GET ──────► │  AuditQueryService /    │──► MongoDB (audit_log)
                                     │  AuditStatsService      │      read template
                                     └────────────────────────┘      (secondary-preferred)

                                     ┌────────────────────────┐
retention trigger ── POST ────────► │  AuditArchiveService    │──► dump file (disk/S3)
(per orgId + cutoff)                │  (dump-then-delete,     │    then MongoDB delete
                                     │   batched)               │    (same write template)
                                     └────────────────────────┘
```

Both ingestion entry points (REST controller and Kafka consumer) call the
exact same `AuditCommandService.ingest(...)` method — idempotency,
mapping, and logging behave identically no matter which transport a
producer chose. This is intentional: it keeps "one audit record shape,
one place duplicates are rejected" true regardless of how an event
arrived.

## 3. CQRS Read/Write Split

This service is write-heavy (every business action across every module
can emit an event) but reads are comparatively rare (support lookups,
dashboards). The codebase is deliberately split along that line:

- **Write side** (`repository.write`, `service.command`): only what
  `MongoRepository` gives for free — `insert`/`save` plus
  `existsByEventId`. No filter/query methods live here, so the hot insert
  path is never tempted to grow expensive lookups.
- **Read side** (`repository.read`, `service.query`): all filtering,
  pagination, and aggregation logic lives here, against a separate
  `MongoTemplate` bean.
- **Two `MongoTemplate` beans** (`MongoReadWriteConfig`):
  - `mongoTemplate` (primary) — used implicitly by Spring Data /
    the write repository. Always talks to the Mongo primary node so
    inserts are immediately durable.
  - `readMongoTemplate` — used explicitly by search/stats
    repositories, `secondaryPreferred` read preference, so heavy
    filter/pagination queries don't compete with the write path for
    primary node resources. (On a single-node dev Mongo instance both
    beans behave identically — this only pays off once secondaries
    exist.)

## 4. Data Model

Single collection: `audit_log`. One document type (`AuditLog`) serves
every module — module-specific fields (storageKey, batchTotalFiles,
deleteType, recipientCount, quotaLevel, etc.) live in a free-form
`metadata` map rather than as dedicated fields, so adding a new module or
event type never requires a schema migration.

Key fields: `eventId` (unique, idempotency key), `module`, `eventType`,
`eventStatus`, `orgId`, `projectId`, `actorType`/`actorId`,
`entityType`/`entityId`, `requestId`/`traceId` (correlation),
`oldValue`/`newValue` (change diff), `metadata`, `occurredAt`
(client-supplied — never trust alone for compliance ordering, can be
skewed/backdated), `recordedAt` (server-generated — authoritative for
ordering and retention).

### Indexes (current)
- `idx_org_project_occurred` — `{orgId, projectId, occurredAt}`
- `idx_org_module_occurred` — `{orgId, module, occurredAt}`
- `idx_org_eventtype_occurred` — `{orgId, eventType, occurredAt}`
- `idx_actor_occurred` — `{actorId, occurredAt}`
- `idx_entity` — `{entityType, entityId, occurredAt}`
- `idx_status_occurred` — `{eventStatus, occurredAt}`
- `eventId` — unique, enforces idempotency

**Note:** retention/archival queries filter by `{orgId, recordedAt}`, not
`occurredAt`. A dedicated `idx_org_recordedAt` compound index is required
for that access pattern to stay indexed rather than falling back to a
collection scan.

## 5. Ingestion Idempotency

- `eventId` has a unique index. `AuditCommandService.ingest()` attempts
  `insert()` and catches `DuplicateKeyException`, translating it to
  `DuplicateAuditEventException`.
- Kafka listener uses **manual ack mode**: offset is committed only after
  a successful write *or* a duplicate rejection. A crash mid-processing
  causes redelivery, never silent data loss. Redelivery is always safe
  because of the unique index — reprocessing the same `eventId` is a
  no-op rejection, not a duplicate row.
- Any other exception during Kafka processing leaves the message unacked
  for retry. A poison-pill message that fails every retry will currently
  stall the consumer on that offset — a dead-letter topic
  (`DeadLetterPublishingRecoverer`) is planned but not yet implemented.

## 6. Retention / Archival Flow

Deliberately **not** a MongoDB TTL index — TTL deletes silently with no
proof of what existed, no archival, and no per-tenant control, which is
unacceptable for audit/compliance data.

Flow (per `orgId` + cutoff):
1. Query the oldest batch (e.g. 1,000) of records matching
   `{orgId, recordedAt < cutoff}`.
2. Write the batch to a compressed NDJSON dump file, then flush.
3. **Only after the flush succeeds**, delete that exact batch by `_id`
   (with `orgId` re-asserted in the delete filter as defense-in-depth
   against cross-tenant deletion).
4. Repeat until no records remain under the cutoff for that org.

This ordering guarantees: a mid-run crash can never lose data that wasn't
already safely on disk. The process is naturally resumable/idempotent —
re-running the same request later simply continues from whatever's left.

## 7. Folder / File Structure

```
src/main/java/com/apargo/service/auditlog/
├── AuditlogApplication.java
├── config/
│   ├── MongoReadWriteConfig.java   # primary + secondary-preferred MongoTemplate beans
│   ├── KafkaConfig.java            # @EnableKafka, conditional on audit.kafka.enabled
│   ├── CorsConfig.java             # dev-only wide-open CORS
│   └── OpenApiConfig.java          # Swagger/OpenAPI metadata
├── controller/v1/
│   ├── AuditIngestController.java  # POST /api/v1/audit-logs
│   ├── AuditQueryController.java   # GET  /api/v1/audit-logs, /{id}
│   └── AuditStatsController.java   # GET  /api/v1/audit-logs/stats
├── consumer/
│   └── AuditKafkaConsumer.java     # single consumer for all audit topics
├── service/
│   ├── command/
│   │   └── AuditCommandService.java   # the one place a row gets written
│   └── query/
│       ├── AuditQueryService.java
│       └── AuditStatsService.java
├── repository/
│   ├── write/
│   │   └── AuditLogWriteRepository.java   # insert/save only, no query methods
│   └── read/
│       ├── AuditLogReadRepository.java
│       ├── AuditLogSearchRepository.java
│       ├── AuditLogSearchRepositoryImpl.java
│       ├── AuditLogStatsRepository.java
│       └── AuditLogStatsRepositoryImpl.java
├── model/
│   └── AuditLog.java                # the single document type for all modules
├── mapper/
│   └── AuditLogMapper.java
├── dto/
│   ├── request/  (AuditIngestRequest, AuditSearchRequest, AuditStatsRequest)
│   └── response/ (AuditEventResponse, AuditStatsResponse, AuditStatsBucket, PagedResponse)
├── enums/
│   ├── AuditEventType.java   # centralized registry, one enum for every module
│   ├── Module.java
│   ├── AuditActorType.java
│   ├── AuditEventStatus.java
│   ├── Device.java
│   ├── DeleteType.java
│   ├── QuotaLevel.java
│   ├── AuditStorageProvider.java
│   └── <Module>AuditEventType.java  # per-module event constant groupings
├── exception/
│   ├── AuditException.java (base)
│   ├── DuplicateAuditEventException.java
│   ├── AuditNotFoundException.java
│   ├── InvalidAuditFilterException.java
│   ├── AuditErrorCode.java
│   ├── AuditErrorResponse.java
│   └── GlobalExceptionHandler.java   # @RestControllerAdvice, maps to AuditErrorResponse
├── validator/base/
│   └── BaseAuditValidator.java
└── util/
    ├── AuditConstants.java
    ├── AuditPageUtil.java
    └── IdempotencyUtil.java

src/main/resources/
├── application.yml        # base config, Mongo URI, Eureka, springdoc, actuator
├── application-dev.yml    # dev port + Mongo override
└── application-kafka.yml  # Kafka bootstrap, consumer group, topic names, dispatch tuning
```

## 8. Configuration / Profiles

- `application.yml` — base: app name, Mongo connection, Eureka client
  registration, springdoc paths, actuator (`health,info` only exposed).
- `application-dev.yml` — dev port (`8070`) and Mongo override.
- `application-kafka.yml` — Kafka bootstrap servers, consumer group,
  manual ack mode, legacy per-module topic names, `audit.kafka.enabled`
  flag (Kafka consumer/config beans are conditional on this).
- Active profiles: `dev` + `kafka`, included via
  `spring.profiles.include` in the base `application.yml`.

**Known gap:** the MongoDB URI (including credentials) is committed in
plaintext in `application.yml`/`application-dev.yml`. This should move to
an environment variable or secrets manager before this is shared more
widely or deployed to a new environment.

## 9. Cross-Service Context

`auditlog` is one microservice within the larger Apargo/Aigreentick
platform, which also includes (non-exhaustive): messaging/broadcast
service, contacts service, template service, storage service,
notification service, and WABA (WhatsApp Business API) service — all
registered via the same Eureka registry. `auditlog` has no outbound
dependency on any of these; it is a pure sink that every other service
writes to.
