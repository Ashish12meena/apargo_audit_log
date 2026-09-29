package com.apargo.services.audit.application.port.out;

import java.time.Instant;
import java.util.List;

import com.apargo.services.audit.common.enums.LogStream;

/** Hot-store operations the archiver needs. */
public interface ArchivableLogStore {

    /** Oldest documents whose {@code archiveAt} is before {@code cutoff}, ordered by archiveAt then id. */
    List<ArchiveRecord> fetchArchivable(LogStream stream, Instant cutoff, int limit);

    /** Deletes exactly these ids, re-checking {@code archiveAt < cutoff}. Returns the number deleted. */
    long deleteArchived(LogStream stream, List<String> eventIds, Instant cutoff);

    /** One document serialized for cold storage (MongoDB Extended JSON, types preserved). */
    record ArchiveRecord(String eventId, Instant occurredAt, String json) {
    }
}
