# PRD — Audit Log Service (`auditlog`)

## 1. Summary

`auditlog` is the centralized audit trail microservice for the Apargo /
Aigreentick platform — a multi-tenant WhatsApp messaging and broadcasting
system. Every other microservice (template, messaging/dispatch,
conversation, storage, wallet, users, projects, plans, WABA) emits audit
events into this service instead of maintaining its own audit table. This
service stores, queries, aggregates, and retires that data.

It exists because compliance, debugging, and customer-support tooling all
need one reliable answer to "what happened, when, and who did it" —
without each producing service reinventing audit storage, or a support
engineer having to check ten different databases.

## 2. Problem Statement

Before this service, audit-style logging was scattered: each module kept
its own event type enum and its own collection. That meant:

- No single place to search "everything that happened to org X" across
  modules.
- Duplicate schema/infra work per module.
- No consistent retention or compliance story.
- Inconsistent event naming and shapes across teams.

## 3. Target Users

| User | Need |
|---|---|
| **Producing microservices** (template, messaging, dispatch, conversation, storage, wallet, users, projects, plans, WABA) | A single, reliable way to emit an audit event — via REST or Kafka — without owning storage. |
| **Internal support/ops engineers** | Search and filter audit history for a specific org/project/actor/entity to debug an incident or answer a customer ticket. |
| **Platform/compliance team** | Aggregate stats (event volume by module/type/status over time) and enforce data retention per organization. |
| **Frontend/admin dashboards** | Paginated, filterable audit log views per tenant. |

This is an **internal, service-to-service and internal-tool** product —
not exposed to end customers directly.

## 4. Goals

- One schema-less, centrally-owned collection for every audit event across
  every module.
- Two ingestion paths: synchronous REST (for producers needing an
  immediate ack/failure signal) and asynchronous Kafka (for high-volume,
  fire-and-forget events) — both funnel through the same command path so
  behavior is identical regardless of transport.
- Idempotent ingestion: retried REST calls or redelivered Kafka messages
  must never create duplicate audit rows.
- Multi-tenant isolation: every event, query, and deletion is scoped by
  `orgId` (and optionally `projectId`); no cross-tenant data leakage.
- Fast filtered search and paginated retrieval for support/debugging.
- Aggregate stats endpoint (counts grouped by module / eventType /
  eventStatus / actorType, bucketed by day or hour) for dashboards.
- Safe, auditable data retention: the ability to archive-then-delete audit
  data older than a given cutoff, per organization, without data loss on
  partial failure.
- Self-describing API contract (OpenAPI/Swagger) since every producing
  team depends on the exact request shape and enum values.

## 5. Non-Goals

- This service does not perform business logic for any module — it is a
  pure record-keeper.
- Not a real-time alerting/SIEM system (no anomaly detection, no
  streaming alerts) — out of scope for now.
- Not a general-purpose logging/metrics pipeline (that's
  Prometheus/Grafana's job) — this is business-event audit trail only,
  not application logs or infrastructure metrics.
- No UI is owned by this service; it only exposes APIs for other
  frontends/admin tools to consume.

## 6. Core Features

### 6.1 Ingestion
- `POST /api/v1/audit-logs` — synchronous REST ingest, returns `201` with
  the created record, or a duplicate/validation error.
- Kafka consumer on `audit.events` (plus legacy per-module topics) —
  manual-ack, idempotent, redelivery-safe. Poison-pill messages are logged
  and left unacked for retry (dead-letter routing is a planned
  improvement, see Architecture doc).
- Every event carries: module, eventType, eventStatus, orgId, projectId,
  actor (type/id), IP/device/user-agent, entity type/id, correlation
  (requestId/traceId), before/after value diffs, free-form metadata, and
  both a client-supplied `occurredAt` and a server-generated `recordedAt`.

### 6.2 Query
- `GET /api/v1/audit-logs` — paginated, multi-filter search (org, project,
  actor, module, event type, entity, status, date range, free-text
  search, sort).
- `GET /api/v1/audit-logs/{id}` — single record lookup.

### 6.3 Stats
- `GET /api/v1/audit-logs/stats` — grouped aggregate counts (mandatory
  `organizationId`, optional project/date range, `groupBy` of 1–2
  whitelisted dimensions, optional day/hour bucketing).

### 6.4 Retention / Archival (in progress)
- Ability to archive-then-purge audit records for a given `orgId` older
  than a given cutoff (explicit timestamp or "N days ago").
- Data is written to a compressed dump file **before** any delete happens;
  a batch is only deleted from MongoDB after its dump write is confirmed
  flushed to disk — no permanent data loss even on mid-run failure.
- Deliberately **not** implemented as a blind MongoDB TTL index, since
  compliance/audit data must be provably archived, not silently dropped.

## 7. Success Criteria

- Zero duplicate audit rows under Kafka redelivery or REST retry.
- No cross-tenant data ever returned from a query or deleted by a purge
  request.
- Query/stats endpoints stay responsive as the collection grows into the
  tens of millions of documents (via correct indexing, see
  Architecture.md).
- A retention run for a given org can be re-run safely (idempotent) and
  never loses data, even if interrupted mid-run.

## 8. Open Questions / Future Work

- Per-organization configurable retention windows (currently retention is
  triggered per-call, not policy-driven/scheduled).
- Dead-letter topic for poison-pill Kafka messages.
- Authentication/authorization on the API (currently CORS is wide open
  for dev; needs origin allow-listing and service-to-service auth before
  production hardening).
- Move dump files from local disk to S3 (or equivalent) for durability
  independent of the service's own VPS disk.
