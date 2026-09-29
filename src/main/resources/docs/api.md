# API — audit-service

Follows the platform API standard: response wrapper, `X-Request-Id`, error codes, UTC ISO-8601.

**Exception stated per the standard (§5):** list endpoints use **cursor pagination** (event logs are
large and live), never page numbers or totals. Health and metrics endpoints (`/actuator/**`) are not
wrapped.

## Headers

| Header | Public `/api/v1/**` | Internal `/internal/v1/**` |
|---|---|---|
| `X-Org-Id` | **required**, positive | optional; `0` = platform-level data |
| `X-Project-Id` | optional; restricts results to that project | optional |
| `X-User-Id` | optional (logging) | optional |
| `X-Request-Id` | optional; generated if missing; always echoed | same |
| `X-Internal-Api-Key`, `X-Internal-Caller` | – | **required** |
| `traceparent` | ignored (a new trace id is created) | continued |

The gateway authenticates the Bearer token and forwards `X-Org-Id` / `X-User-Id`; this service
trusts those headers and must only be reachable through the gateway or the internal network.

CORS is open to all origins by default (`AUDIT_CORS_ALLOWED_ORIGINS=*`); set a comma-separated list
of origins to restrict it. `X-Request-Id` is exposed to browsers.

## Endpoints

### `GET /api/v1/audit-logs`

| Parameter | Notes |
|---|---|
| `module`, `eventType`, `entityType` | UPPER_SNAKE_CASE |
| `entityId` | requires `entityType` |
| `status` | `SUCCESS` \| `FAILURE` |
| `channel` | `WEB` \| `API` \| `MOBILE` \| `WORKER` |
| `actorId`, `sourceService`, `errorCode` | exact match |
| `traceId` | 32 lowercase hex |
| `from`, `to` | ISO-8601; default last 7 days; range ≤ 31 days; `from` inclusive, `to` exclusive |
| `size` | 1–100, default 20 |
| `cursor` | `nextCursor` of the previous page; omit for the first page |

Sorted by `occurredAt` desc, then `eventId` desc.

```json
{
  "success": true, "status": 200, "code": "SUCCESS",
  "message": "Audit logs fetched successfully",
  "data": {
    "items": [ {
      "eventId": "0192f1a2-7c3e-7001-9f00-000000000001", "schemaVersion": 1,
      "sourceService": "template-service", "module": "TEMPLATE", "eventType": "TEMPLATE_UPDATED",
      "status": "SUCCESS", "orgId": 1001, "projectId": 2001,
      "actor": { "type": "USER", "id": "501" },
      "entity": { "type": "TEMPLATE", "id": "tpl-10001", "name": "order_confirmation" },
      "changes": [ { "field": "status", "oldValue": "DRAFT", "newValue": "APPROVED" } ],
      "metadata": { "wabaId": "waba-001" },
      "channel": "WEB", "requestId": "req-7a92", "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
      "occurredAt": "2026-09-25T08:45:20Z", "recordedAt": "2026-09-25T08:45:21Z"
    } ],
    "pagination": { "size": 20, "nextCursor": "djE6MTc1ODc4…", "hasNext": true }
  },
  "errors": [],
  "meta": { "requestId": "3f2a…", "traceId": "…", "timestamp": "2026-09-28T11:00:00Z" }
}
```

### `GET /api/v1/audit-logs/{eventId}`

One audit log of the caller's organization (and project, if `X-Project-Id` is sent).
`404 AUDIT_LOG_NOT_FOUND` otherwise.

### `GET /api/v1/audit-logs/stats`

| Parameter | Notes |
|---|---|
| `groupBy` | **required**, 1–2 of `module`, `eventType`, `status`, `channel`, `sourceService`, `actorType`, `projectId` |
| `bucket` | optional `hour` \| `day` (UTC) |
| `from`, `to` | as above |

`data`: `{ from, to, groupBy, bucket, totalEvents, truncated, results: [ { dimensions, period, count } ] }`,
sorted by count desc; at most 1000 groups (`truncated: true` when more existed).

### `GET /api/v1/access-logs`, `GET /api/v1/access-logs/{eventId}`

Filters: `eventType`, `status`, `actorId`, `traceId`, `from`, `to`, `size`, `cursor`.
`404 ACCESS_LOG_NOT_FOUND` for an unknown id.

### Internal (`X-Internal-Api-Key`)

| Endpoint | Notes |
|---|---|
| `GET /internal/v1/audit-logs` | same filters; without `X-Org-Id` a `traceId` is required |
| `GET /internal/v1/audit-logs/trace/{traceId}` | every event of one trace, across services |
| `GET /internal/v1/audit-logs/{eventId}` | any organization unless `X-Org-Id` is sent |
| `GET /internal/v1/access-logs` | without `X-Org-Id` a `traceId` or `actorId` is required |
| `GET /internal/v1/access-logs/{eventId}` | any organization unless `X-Org-Id` is sent |

## Errors

| HTTP | `code` | When |
|---|---|---|
| 400 | `BAD_REQUEST` | missing/invalid `X-Org-Id`, `X-Project-Id`, `X-User-Id`; unscoped internal search |
| 401 | `UNAUTHENTICATED` | internal route without a valid API key |
| 403 | `FORBIDDEN` | `X-Org-Id: 0` on a public route; caller not in the allow-list |
| 404 | `NOT_FOUND`, `AUDIT_LOG_NOT_FOUND`, `ACCESS_LOG_NOT_FOUND` | unknown route or id |
| 405 | `METHOD_NOT_ALLOWED` | only `GET` is supported |
| 422 | `VALIDATION_FAILED` | invalid parameter; `errors[]` lists `field`, `code`, `message` |
| 500 | `INTERNAL_ERROR` | unexpected |
| 503 | `SERVICE_UNAVAILABLE` | MongoDB unavailable; `Retry-After` set |
| 504 | `TIMEOUT` | query exceeded its time limit; narrow the range or add filters |

Field error codes: `REQUIRED`, `INVALID_FORMAT`, `INVALID_VALUE`, `TOO_LONG`, `OUT_OF_RANGE`.
