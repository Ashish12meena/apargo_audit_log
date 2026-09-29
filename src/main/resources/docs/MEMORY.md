# Memory — audit-service

Read this first. It records the current state and the decisions already made.

## Current state (v1.0.0)

- New implementation replacing the previous `auditlog` service (REST ingest + single `audit_log`
  collection). The old collection is dev data and is not migrated.
- Done: audit and access ingestion from Kafka, DLQ, circuit breaker, MongoDB schema/indexes,
  read APIs (public + internal), stats, archiver skeleton with `NONE` / `LOCAL` stores, health,
  metrics, unit tests, Testcontainers pipeline test.
- Not done (by decision): cold-storage target (S3/MinIO or Atlas Online Archive), DLQ replay tool,
  daily stats rollups, `activity_events`.

## Decisions

| Decision | Why |
|---|---|
| Kafka is the only ingestion path | One write path with batching, backpressure and DLQ; no unauthenticated writes |
| Separate topics and consumer groups per category | Contract rule; independent scaling and pausing |
| `_id = eventId` | Idempotency without an extra unique index |
| camelCase contract names in MongoDB | One name from producer to API; metadata is camelCase anyway |
| `access_logs` keeps the full schema-doc shape | Producers may add those fields later; absent fields cost nothing |
| Batching through Kafka fetch settings, not an in-memory buffer | No data held outside Kafka's commit protocol |
| Transient failures retried forever with back-off; DLQ only for poison | Outages must not turn into data loss or DLQ floods |
| Non-blocking contract problems are stored and reported | Losing an audit record is worse than an incomplete one |
| `archiveAt` + archiver, no TTL | TTL would delete unarchived data |
| Org scope only from headers | API standard |
| Every setting has a default, in YAML and in Java | Service always starts; non-dev instances on local defaults log `audit_config_warning` |
| Cursor paging, no totals | Standard allows it for event logs; counts are too expensive at scale |

## Open items

- Choose the cold-storage target and total retention, then add its `ArchiveStore`.
- Confirm which topic name template-service uses in production (`platform.audit.events` is the
  contract default).
- Propose `AccessEventValidator` and a published contract jar to the platform owners.

## Change log

- 1.0.0 — first release of the redesigned service.
- 1.0.1 — every setting has a default: `AuditDefaults` (Java) = `application.yml` (`${ENV:default}`,
  prod included), checked by `AuditPropertiesDefaultsTest`; startup warns outside development when
  values are still on local defaults.
- 1.0.2 — no empty defaults: Kafka/Eureka default to 146.88.24.113, read URI defaults to the write URI,
  internal API key has a placeholder default (warned outside development), allowed callers `*`,
  CORS `*` on all routes. Docs moved to `src/main/resources/docs`; `src/main/resources/db/` holds
  `mongo/*.json` and `audit_schema.sql` (MySQL reference of the collections).
