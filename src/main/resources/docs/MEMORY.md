# MEMORY.md — Project State Checkpoint

**Purpose:** read this file first, before scanning the codebase. It exists
so a new session (human or AI) doesn't have to re-open every file to
figure out what's done, what's mid-flight, and what decisions were
already made. Update this file at the end of any work session — treat it
as more current than PHASES.md's checkboxes if they ever disagree.

---

## Currently being worked on

**File:** `AuditArchiveService.java` (does not exist in the repo yet —
designed in conversation, not yet created)
**Feature:** Phase 3 — Retention/Archival
**State:** Design agreed, code drafted and reviewed, **nothing committed
to `src.zip` yet**. Next session should start by creating the actual
files listed below, not by re-deriving the design.

## Last completed

- Phase 2 (Query & Stats) — fully implemented in repo, verified present:
  `AuditQueryController`, `AuditStatsController`, `AuditQueryService`,
  `AuditStatsService`, `AuditLogSearchRepository(Impl)`,
  `AuditLogStatsRepository(Impl)`.
- Retention **design** finalized (see Decisions Log) — dump-then-delete,
  batched, scoped per `orgId`.

## Not yet started

Everything in Phase 4 (Security & Hardening), Phase 5
(Reliability/Observability), Phase 6 (Scale & Ops) in `PHASES.md`.
Nothing in those phases has been designed in detail yet, only listed as
future work.

---

## Decisions Log (don't re-litigate these)

| Decision | Why | Date/context |
|---|---|---|
| Retention uses dump-then-delete, **not** MongoDB TTL index | TTL deletes silently, no archival proof, no per-tenant control — unacceptable for audit/compliance data | Retention design discussion |
| Retention is scoped per `orgId`, not a global purge | Limits blast radius; matches multi-tenant isolation model; allows future per-org policy differences | Same discussion |
| Purge by `recordedAt`, not `occurredAt` | `occurredAt` is client-supplied and can be skewed/backdated; `recordedAt` is server-generated and authoritative | Same discussion |
| Batch delete only re-queries the same filter (no skip/offset, no manual cursor tracking) | Deleted records naturally drop out of the next iteration's result set — simpler and avoids skip/offset performance decay at scale | Same discussion |
| Delete query re-asserts `orgId` even though `_id` list already came from that org | Defense-in-depth against any future concurrent-call bug crossing tenants | Same discussion |
| Dump format: gzip-compressed NDJSON, one file per run | Streamable, no need to hold full array in memory, compresses well for archival | Same discussion |
| Local disk for dump files is acceptable for now | Matches current VPS deployment; flagged in PHASES.md Phase 6 to move to S3 later | Same discussion |
| `AuditCommandService.ingest()` remains the only write entry point | Keeps idempotency/logging identical regardless of REST vs Kafka transport | Established earlier, Phase 1 |

---

## Known gaps / flagged issues (not yet fixed, don't re-discover)

- `application.yml` / `application-dev.yml` have a live MongoDB URI with
  credentials committed in plaintext. Not fixed. Tracked in Phase 4.
- `CorsConfig` is wide open (`*`) — dev-only by design, tracked in Phase 4.
- No auth on any endpoint (ingest/query/stats/future archive). Tracked in
  Phase 4.
- Kafka consumer has no dead-letter handling — a permanently-failing
  message stalls the consumer on that offset. Tracked in Phase 5.
- Existing compound indexes are all keyed on `occurredAt`; retention
  needs a **new** `{orgId, recordedAt}` index that does not exist yet.

---

## How to resume work efficiently

1. Read this file.
2. Check `PHASES.md` for the phase you're resuming.
3. Only open the specific files listed under "Currently being worked on"
   or the relevant phase's checklist — do not re-scan the full repo tree.
4. When a checklist item is finished, update both `PHASES.md` (check the
   box) and this file (move it from "Currently being worked on" /
   "Not yet started" into "Last completed", and add any new decision to
   the Decisions Log if one was made).
