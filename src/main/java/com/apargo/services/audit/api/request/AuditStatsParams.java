package com.apargo.services.audit.api.request;

import java.time.Instant;

/**
 * Query parameters of the stats endpoint.
 *
 * @param groupBy comma-separated, 1–2 of: module, eventType, status, channel, sourceService, actorType, projectId
 * @param bucket  optional time dimension: {@code hour} or {@code day}
 */
public record AuditStatsParams(String groupBy, String bucket, Instant from, Instant to) {
}
