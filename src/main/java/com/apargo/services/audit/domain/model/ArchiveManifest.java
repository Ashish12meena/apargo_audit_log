package com.apargo.services.audit.domain.model;

import java.time.Instant;

import com.apargo.services.audit.common.enums.ArchiveStatus;

/** One exported archive file and what happened to the documents in it. */
public record ArchiveManifest(
        String id,
        String collection,
        String runId,
        String dt,
        String objectKey,
        long docCount,
        long bytes,
        String sha256,
        String firstId,
        String lastId,
        Instant minOccurredAt,
        Instant maxOccurredAt,
        ArchiveStatus status,
        Instant exportedAt,
        Instant deletedAt,
        String error) {
}
