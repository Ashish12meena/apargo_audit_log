# Rules — audit-service

## Contract and schema

1. `com.apargo.platform.contract` is copied **unchanged**. Update it together with every service;
   replace the copy with the shared jar once it exists (imports stay the same).
2. Stored field names are the contract's names (camelCase). Never rename a stored field; only add.
   Every name lives in `MongoFields`.
3. Collection shape and indexes change only in `src/main/resources/db/mongo/*.json`. Add an index only
   for a real query pattern and review its write cost; every timeline index ends in
   `occurredAt:-1, _id:-1`.
4. Documents are append-only. Only the archiver deletes, and only ids in a verified export.
   No TTL index on `audit_logs` / `access_logs`.

## Ingestion

5. `IngestBatchService` is the only write path. Nothing else inserts log documents.
6. Poison records are dead-lettered inside the batch; the listener throws only for transient
   failures. Never dead-letter because a dependency is down; never skip a record.
7. Blocking rules cover only what storage and queries depend on; everything else is reported and
   stored. A new rule is blocking only if the document would be unusable without it.
8. `module`, `eventType`, `status` (access) and `actor.type` (access) are validated by format, not
   by list: a new value in a producer needs no change here.

## API

9. Tenant scope comes only from `X-Org-Id` / `X-Project-Id`, never from parameters or bodies.
10. Every response uses `ApiResponse`; every error goes through `GlobalExceptionHandler` (or
    `ErrorResponseWriter` in filters). Error codes live in `ErrorCode`.
11. Every list query is time-bounded, cursor-paged and served by an index; every read sets `maxTimeMS`.
12. Messages never contain stack traces, driver messages, payloads or secrets.

## Code

13. `domain` and `application` stay free of Spring, Kafka and MongoDB types; wire them in
    `ApplicationConfig`. Adapters implement `application.port.out` interfaces.
14. No hard-coded names or tunables: constants in `common.constant`, settings in `AuditProperties`.
    Every setting has a default, defined once in `AuditDefaults` and repeated in `application.yml`
    as `${ENV_VAR:default}`; annotation placeholders use `AuditDefaults.Placeholders`. A new setting
    needs all three plus a line in `AuditPropertiesDefaultsTest` if it is not covered by equality.
    No empty defaults. Secrets get a placeholder default that must be overridden per environment;
    startup logs `audit_config_warning` outside development while a default secret is in use.
15. Logs are key=value with a stable event name (`ingest_dead_lettered stream=… reason=…`); MDC
    carries `requestId`, `traceId`, `orgId`, `stream`.
16. Tests: domain and application logic with plain JUnit; web layer with standalone MockMvc; the
    pipeline with Testcontainers (`-Pit`).
