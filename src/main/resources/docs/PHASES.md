# PHASES.md — Build Phases for `auditlog`

Purpose: a scannable roadmap so future work (by you or an AI assistant)
can jump straight to the relevant phase instead of re-reading the whole
codebase. Pair with `MEMORY.md` for current status/checkpoint.

---

## Phase 0 — Foundation ✅ DONE
Core scaffolding all other phases depend on.
- [x] Spring Boot project setup, Eureka client registration
- [x] `AuditLog` document model (schema-less, single collection for all modules)
- [x] Centralized enums: `AuditEventType`, `Module`, `AuditActorType`,
      `AuditEventStatus`, `Device`, `DeleteType`, `QuotaLevel`,
      `AuditStorageProvider`
- [x] `MongoReadWriteConfig` — primary (write) + secondary-preferred (read) `MongoTemplate` beans
- [x] Compound indexes on `AuditLog` (org+project+occurredAt, org+module+occurredAt, org+eventType+occurredAt, actor+occurredAt, entity, status+occurredAt)
- [x] `GlobalExceptionHandler` + `AuditException` hierarchy + `AuditErrorCode`

## Phase 1 — Ingestion ✅ DONE
Getting events into the system, exactly once.
- [x] `AuditCommandService.ingest()` — single write path for all transports
- [x] `AuditLogWriteRepository` — insert-only, no query methods (CQRS write side)
- [x] Idempotency via unique `eventId` index + `DuplicateAuditEventException`
- [x] `AuditIngestController` — `POST /api/v1/audit-logs` (sync REST path)
- [x] `AuditKafkaConsumer` — single consumer for all audit topics, manual ack,
      redelivery-safe
- [x] `KafkaConfig` — conditional on `audit.kafka.enabled`

## Phase 2 — Query & Stats ✅ DONE
Reading the data back out.
- [x] `AuditQueryService` + `AuditLogSearchRepository`/`Impl` — filtered,
      paginated search (`GET /api/v1/audit-logs`, `GET /{id}`)
- [x] `AuditStatsService` + `AuditLogStatsRepository`/`Impl` — grouped
      aggregates (`GET /api/v1/audit-logs/stats`)
- [x] `PagedResponse<T>` wrapper, `AuditPageUtil`
- [x] OpenAPI/Swagger docs (`OpenApiConfig`)
- [x] Dev-mode CORS (`CorsConfig` — wide open, flagged for tightening in Phase 4)

## Phase 3 — Retention / Archival 🔄 IN PROGRESS
Safe, per-tenant deletion of old audit data without data loss.
- [x] Design finalized: dump-then-delete, batched, per-`orgId` + cutoff
      (rejected plain MongoDB TTL — no archival proof, no per-tenant control)
- [x] Batch algorithm designed: query oldest N → write to gzip NDJSON →
      flush → delete same batch by `_id` (with `orgId` re-asserted) → repeat
- [ ] `AuditArchiveRequest` / `AuditArchiveResponse` DTOs — **not yet added to repo**
- [ ] `AuditArchiveService` — **not yet added to repo** (designed in chat, not committed)
- [ ] `AuditArchiveController` — `POST /api/v1/audit/archive` — **not yet added**
- [ ] `AuditArchiveException` — **not yet added**
- [ ] New index: `idx_org_recordedAt` on `{orgId, recordedAt}` — **not yet added to `AuditLog.java`**
      (required — existing indexes are keyed on `occurredAt`, not `recordedAt`)
- [ ] `audit.archive.dump-dir` config property — **not yet added to `application.yml`**
- [ ] Emit a self-audit event on purge completion (module=SYSTEM,
      eventType=AUDIT_PURGE_COMPLETED) — **not yet added, not yet in `AuditEventType`**

**Next concrete step:** wire the above into the actual codebase — currently
only exists as reviewed/agreed-upon code in conversation, not in `src.zip`.

## Phase 4 — Security & Hardening ⬜ NOT STARTED
- [ ] Lock down `CorsConfig` to real frontend origins (currently `*`)
- [ ] Service-to-service auth on ingest/query/archive endpoints (currently
      none — any caller that can reach the service can read/write/purge)
- [ ] Move Mongo URI (and any future Kafka/S3 credentials) out of
      `application.yml`/`application-dev.yml` into env vars or a secrets
      manager — currently committed in plaintext
- [ ] Restrict actuator exposure beyond `health,info` review

## Phase 5 — Reliability & Observability ⬜ NOT STARTED
- [ ] Dead-letter topic for poison-pill Kafka messages (currently a
      permanently-failing message just stalls the consumer on that offset)
- [ ] Metrics/alerting on ingest failure rate, consumer lag, purge volume
- [ ] Structured audit-of-the-auditor: alert if purge deletes an unusually
      large batch (possible misconfigured cutoff)

## Phase 6 — Scale & Ops ⬜ NOT STARTED (future)
- [ ] Move archive dump files from local disk to S3 (matches existing
      `storage-service` pattern) for durability independent of the VPS disk
- [ ] Per-organization configurable retention policy (currently retention
      is triggered per explicit API call, not a stored policy)
- [ ] Scheduled/automatic retention runs per org policy (only after policy
      storage exists — do not build a blind global cron purge)
- [ ] Async job pattern (`202 Accepted` + job id + status polling) for
      archive runs once data volume makes synchronous calls impractical

---

### Status legend
✅ DONE · 🔄 IN PROGRESS · ⬜ NOT STARTED
