# audit-service

Central store for the platform's **audit logs** (business changes) and **access logs** (sign-ins,
access decisions). Services publish events to Kafka; this service validates them, writes them to
MongoDB in batches, serves tenant-scoped read APIs, and (optionally) moves old data to cold storage.

- Architecture, schema, indexes, retention: [`src/main/resources/docs/architecture.md`](src/main/resources/docs/architecture.md)
- HTTP API (headers, endpoints, paging, errors): [`src/main/resources/docs/api.md`](src/main/resources/docs/api.md)
- Rules for changing this service: [`src/main/resources/docs/rules.md`](src/main/resources/docs/rules.md)
- Current state and change log: [`src/main/resources/docs/memory.md`](src/main/resources/docs/memory.md)

## Requirements

- Java 21, Maven 3.9+
- MongoDB 6.0+ **replica set** (write concern `majority`; a single-node replica set is fine locally)
- Kafka 3.x
- Docker, only for the integration tests

## Run locally

```bash
# MongoDB as a single-node replica set (Kafka and Eureka default to the shared server 146.88.24.113)
docker run -d --name audit-mongo -p 27017:27017 mongo:7.0 --replSet rs0
docker exec audit-mongo mongosh --quiet --eval "rs.initiate({_id:'rs0',members:[{_id:0,host:'localhost:27017'}]})"

mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile uses MongoDB on `localhost`, Kafka and Eureka on `146.88.24.113` (set
`KAFKA_BOOTSTRAP_SERVERS` / `EUREKA_DEFAULT_ZONE` / `EUREKA_ENABLED=false` to change that), sets the internal
API key to `dev-internal-key`, and writes archive files (if the archiver role is on) to `./build/archive`.

Try it:

```bash
curl -s localhost:8070/api/v1/audit-logs -H 'X-Org-Id: 1001' | jq
curl -s localhost:8070/actuator/health | jq
```

## Build and test

```bash
mvn clean verify            # compile + unit tests
mvn clean verify -Pit       # + Testcontainers integration test (Kafka + MongoDB), needs Docker
```

## Configuration

Every setting is an environment variable with a default, so the service starts with nothing set.
Values are read, highest first, from: real environment variables, `./.env` (this service's own
settings and secrets), `../platform.env` (shared infrastructure addresses for all services — see the
platform docs, `docs/configuration/`), then the defaults in `application.yml`.
Defaults are defined once in `AuditDefaults.java`: `application.yml` repeats them as
`${ENV_VAR:default}`, `AuditProperties` falls back to them when a key is missing, and
`AuditPropertiesDefaultsTest` fails if the two differ. `.env.example` lists what to set per
environment. Outside `development`, startup logs an `audit_config_warning` for every value still on
its default (MongoDB on localhost, the default internal API key).

| Variable | Default | Meaning |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | none | `dev` locally, `prod` for staging/production |
| `AUDIT_ENVIRONMENT` | `development` | Events from any other environment are dead-lettered |
| `KAFKA_BOOTSTRAP_SERVERS` | `146.88.24.113:9092` | Brokers |
| `EUREKA_DEFAULT_ZONE` | `http://146.88.24.113:8761/eureka/` | Service registry (`EUREKA_URL` still honoured) |
| `AUDIT_TOPIC` / `ACCESS_TOPIC` | `platform.audit.events` / `platform.access.events` | Contract defaults (`EventTopics`) |
| `AUDIT_MONGO_WRITE_URI` / `AUDIT_MONGO_READ_URI` | `mongodb://localhost:27017/?replicaSet=rs0` / the write URI | Separate users and pools for writes and reads |
| `AUDIT_HOT_DAYS` / `ACCESS_HOT_DAYS` | `30` / `30` | Days a document stays in MongoDB before it may be archived |
| `AUDIT_ROLE_CONSUMER` / `_API` / `_ARCHIVER` | `true` / `true` / `false` | Which parts this instance runs |
| `AUDIT_ARCHIVE_STORE` | `NONE` | `NONE` never deletes anything; `LOCAL` is for dev/tests |
| `AUDIT_INTERNAL_API_KEY` | `apargo-audit-internal-key` | Required for `/internal/**`; **override it** in every shared environment |
| `AUDIT_INTERNAL_ALLOWED_CALLERS` | `*` | Caller names allowed on `/internal/**`; `*` = any caller with the key |
| `AUDIT_CORS_ALLOWED_ORIGINS` | `CORS_ALLOWED_ORIGINS`, else `*` | Browser origins; `*` = all, comma-separated list to restrict |
| `AUDIT_MONGO_MANAGE_SCHEMA` | `true` | Create collections, validators and indexes at startup |

Batching, retry, circuit breaker, query limits and archiver tuning are in `application.yml`.

## Roles

One artifact, three roles, switched by configuration:

| Role | Does | Scale by |
|---|---|---|
| consumer | Kafka → MongoDB ingestion, DLQ | Partitions and consumer instances (threads ≤ partitions) |
| api | `/api/v1/**`, `/internal/v1/**` | Instances behind the gateway |
| archiver | Scheduled export → verify → delete | One active at a time (MongoDB lease lock) |

Small deployments run all three in one process; split them when load differs.

## Database files

`src/main/resources/db/`:

- `mongo/*.json`: the runtime definition of each collection (validator + indexes), applied at startup.
- `audit_schema.sql`: the same collections as MySQL tables (columns, indexes, constraints), for
  documentation, reporting exports and analysis. Nested fields are flattened (`actor.id` → `actorId`).

## Database users

With `AUDIT_MONGO_MANAGE_SCHEMA=true` the writer user needs `createCollection`, `createIndex` and
`collMod` on the `audit` database. Otherwise set it to `false` and let a DBA run
`scripts/apply-mongo-schema.js` (same JSON files). Least-privilege users:

- writer: `insert`, `find` on `audit_logs`, `access_logs`; `find`, `insert`, `update` on `job_locks`
- reader: `find` on `audit_logs`, `access_logs`
- archiver (if split out): `find`, `remove` on the log collections; `insert`, `update` on `archive_manifests`, `job_locks`

## Operations

- **Health**: `/actuator/health/liveness`, `/actuator/health/readiness` (includes MongoDB).
- **Metrics**: `/actuator/prometheus`. Alert on consumer lag, `audit_consumer_circuit_open == 1`,
  any `audit_ingest_dead_letters_total` increase in production, and archiver failures.
- **Dead letters**: `<topic>.dlq` keeps the original key, value and headers plus `x-dlq-*`
  headers (reason, detail, original topic/partition/offset). Fix the producer (or upgrade this
  service's contract copy), then re-publish the records to the source topic; ingestion is
  idempotent on `eventId`.
- **Kafka retention** of the main topics must stay shorter than the hot window (default 7 d < 30 d),
  so a redelivered event always meets its original in MongoDB and is de-duplicated.
