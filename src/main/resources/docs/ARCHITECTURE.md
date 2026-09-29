# Architecture — audit-service

## 1. Purpose and boundaries

- **In**: `AuditEventDto` on the audit topic (business services) and `AccessEventDto` (+ optional
  extras) on the access topic (auth service, gateway). Contract: `com.apargo.platform.contract`,
  copied unchanged under `src/main/java`.
- **Out**: `audit_logs` and `access_logs` in MongoDB, read APIs, dead-letter topics, cold archives.
- **Not here**: producing events, outbox relays, business logic of any module.

## 2. Layers

```
com.apargo.services.audit
├── common/          constants, enums, error codes, response wrapper; no business logic
├── domain/          models, acceptance rules, mapping, retention; no Spring / Kafka / Mongo
├── application/     use cases (ingest, query, stats, archive) + ports (port/out)
├── infrastructure/  adapters: Kafka, MongoDB, archive stores, scheduling, health, metrics, config
└── api/             HTTP: controllers, request/response records, context filters, error handling
```

Dependencies point inwards: `api` and `infrastructure` → `application` → `domain` → `common`.
The domain and application classes are plain Java, wired in `infrastructure/config/ApplicationConfig`.

## 3. Ingestion pipeline

```
Kafka poll (≤ max.poll.records, or fetch.min.bytes / fetch.max.wait)
  → StreamBatchHandler: circuit check → InboundEvent (raw bytes kept)
  → IngestBatchService (the only write path):
       size check → schemaVersion header → decode (private ObjectMapper) → rules → factory
       → LogWriter.write: one unordered insertMany, w:majority, j:true
       → dead-letter poison + store-rejected records, wait for broker acks
  → listener returns → container commits the batch offsets
```

| Situation | Outcome |
|---|---|
| Valid event | stored (`_id = eventId`) |
| Same `eventId` again (redelivery, producer retry, replay) | duplicate key 11000 → counted, not an error |
| Bad JSON, unknown enum value, blocking rule failed, wrong env, unsupported version, oversized | DLQ with reason; batch continues |
| MongoDB refuses one document (validation, size) | that record → DLQ `DB_REJECTED` |
| MongoDB / DLQ unavailable, write-concern error | exception → whole batch retried in memory with exponential back-off, no attempt limit, consumer paused but polling |
| 5 consecutive failed batches | circuit opens: batches fail fast (no store calls) for `open-duration`, then one trial |

**Blocking rules** (DLQ): `eventId` UUIDv7, `schemaVersion` supported, `environment` equal to ours,
`sourceService`, `eventType` / `module` format, `status`, `orgId ≥ 0`, `actor.type`, `occurredAt`
(≤ 24 h in the future), ≤ 100 changes. Every other finding of the contract's `AuditEventValidator`
is **reported** (warning + `audit.ingest.contract.violations`) and the event is stored anyway.

**Backpressure**: Kafka is the buffer. A thread polls again only after its batch is stored, so
in-memory data is bounded by `threads × max.poll.records`. No async writes, no unbounded queues;
nothing is dropped under load.

**Concurrency**: key = `eventId` (even spread, ordering not needed); one consumer group per topic;
cooperative-sticky assignment; listener threads across instances ≤ partitions.

## 4. MongoDB

Database `audit`. Collections are defined once in `src/main/resources/db/mongo/*.json` (validator +
indexes), applied at startup by `MongoSchemaInitializer` or by `scripts/apply-mongo-schema.js`.
`src/main/resources/db/audit_schema.sql` describes the same collections as MySQL tables
(nested fields flattened) for documentation, reporting exports and analysis.

### audit_logs (mirrors `AuditEventDto`)

```js
{
  _id: "<eventId UUIDv7>", schemaVersion: 1, sourceService, module, eventType,
  status: "SUCCESS" | "FAILURE", orgId: NumberLong, projectId?: NumberLong,
  actor: { type, id?, name?, impersonatorId? },
  entity?: { type, id?, name? },
  changes: [ { field, oldValue?, newValue? } ],     // always present; a null value is omitted
  metadata: { ... },                                // always present, keys as sent
  error?: { category, code, message?, details?, reference? },
  channel?, requestId?, traceId?, ip?, userAgent?,
  occurredAt: Date,                                 // producer time
  recordedAt: Date,                                 // server time
  archiveAt: Date                                   // recordedAt + hot days
}
```

Indexes: `org_time`, `org_project_time`, `org_entity_time`, `org_actor_time`, `org_module_time`,
`org_failures_time` (partial: FAILURE), `trace`, `archive_scan`. Timeline indexes end in
`occurredAt:-1, _id:-1`, the keyset-pagination sort.

### access_logs (every `AccessEventDto` field + optional schema-doc fields)

```js
{
  _id, schemaVersion, sourceService, eventType, status, orgId?,
  actor: { type, id?, name?, email?, impersonatorId? },
  authMethod?, failureCode?, resource?, ip?, userAgent?, country?, metadata?,
  requestId?, traceId?, occurredAt, recordedAt, archiveAt
}
```

`failureReason` in incoming JSON is accepted as an alias of `failureCode`. Indexes: `org_time`,
`actor_time`, `org_type_time`, `actor_email_status_time` (partial), `trace` (partial), `archive_scan`.

### Other collections

- `archive_manifests`: one document per exported archive file (status EXPORTED → DELETED | FAILED).
- `job_locks`: lease lock for the archiver.

### Clients

Two `MongoClient`s with separate pools: **write** (primary, `w:majority, j:true`, write timeout,
retryable writes) and **read** (`secondaryPreferred`, max staleness 90 s). Every read sets `maxTimeMS`.

## 5. Retention and cold storage

- `archiveAt` is written on every document; **no TTL index** on the log collections (a TTL would
  delete data even when archiving has not happened).
- `ArchiveService` (archiver role, cron, one instance via `job_locks`): fetch by `archiveAt` →
  write gzip NDJSON (Extended JSON) per UTC day of `occurredAt` → verify → manifest → delete exactly
  those ids, in chunks with pauses. A crash between export and delete re-exports (duplicate in cold
  storage, never a loss).
- `ArchiveStore` is pluggable. Default `NONE`: the archiver does nothing and data stays in MongoDB.
  `LOCAL` is for dev/tests. Adding S3/MinIO is one new class; with Atlas Online Archive the archiver
  stays off and Atlas uses `archiveAt`.
- Kafka main-topic retention must stay shorter than the hot window, so redeliveries are always
  de-duplicated against a document that is still hot.

## 6. Read path

Tenant scope comes only from headers (`X-Org-Id`, `X-Project-Id`). Lists use cursor paging
(newest first), a bounded time range (default 7 d, max 31 d), no total counts. Internal routes
(`/internal/**`, API key) can read any organization, including platform-level data (`orgId 0`);
without `X-Org-Id` they must be narrowed by trace id (or actor id for access logs).

## 7. Operability

- Health: liveness, readiness (includes `auditMongo`), `auditConsumer` details (circuit, containers).
- Metrics: `audit.ingest.*`, `audit.consumer.circuit.open`, `audit.archive.*`.
- Logs: key=value messages; MDC `requestId`, `traceId`, `orgId`, `stream`; ECS JSON in `prod`.
- Shutdown: graceful HTTP, listener containers finish the in-flight batch, then clients close.
