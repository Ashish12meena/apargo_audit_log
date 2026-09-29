package com.apargo.services.audit.application.archive;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apargo.services.audit.application.port.out.ArchivableLogStore;
import com.apargo.services.audit.application.port.out.ArchivableLogStore.ArchiveRecord;
import com.apargo.services.audit.application.port.out.ArchiveManifestStore;
import com.apargo.services.audit.application.port.out.ArchiveStore;
import com.apargo.services.audit.application.port.out.ArchiveStore.StoredObject;
import com.apargo.services.audit.application.port.out.AuditMetrics;
import com.apargo.services.audit.common.enums.ArchiveStatus;
import com.apargo.services.audit.common.enums.LogStream;
import com.apargo.services.audit.domain.model.ArchiveManifest;

/**
 * Moves documents past their {@code archiveAt} to cold storage: export → verify → record the
 * manifest → delete exactly the exported ids. A document is never deleted unless it is in a
 * verified export. With a disabled store the run does nothing, so data simply stays hot.
 * <p>
 * A crash between export and delete re-exports those documents on the next run (a duplicate in
 * cold storage, never a loss); cold-storage readers de-duplicate on {@code _id}.
 */
public final class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    private static final DateTimeFormatter RUN_ID_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final String OBJECT_SUFFIX = ".ndjson.gz";

    private final ArchivableLogStore source;
    private final ArchiveStore store;
    private final ArchiveManifestStore manifests;
    private final AuditMetrics metrics;
    private final Clock clock;
    private final Settings settings;

    public ArchiveService(ArchivableLogStore source, ArchiveStore store, ArchiveManifestStore manifests,
                          AuditMetrics metrics, Clock clock, Settings settings) {
        this.source = Objects.requireNonNull(source);
        this.store = Objects.requireNonNull(store);
        this.manifests = Objects.requireNonNull(manifests);
        this.metrics = Objects.requireNonNull(metrics);
        this.clock = Objects.requireNonNull(clock);
        this.settings = Objects.requireNonNull(settings);
    }

    public RunSummary run() {
        Instant started = clock.instant();
        String runId = RUN_ID_FORMAT.format(started);
        if (!store.isEnabled()) {
            log.info("archive_run_skipped runId={} reason=store_disabled", runId);
            metrics.archiveRun("skipped");
            return new RunSummary(runId, "skipped", 0, 0);
        }

        Instant deadline = started.plus(settings.maxRun());
        long exported = 0;
        long deleted = 0;
        for (LogStream stream : LogStream.values()) {
            long[] counts = archiveStream(stream, runId, deadline);
            exported += counts[0];
            deleted += counts[1];
        }
        metrics.archiveRun("completed");
        log.info("archive_run_completed runId={} store={} exported={} deleted={} tookMs={}", runId,
                store.describe(), exported, deleted, Duration.between(started, clock.instant()).toMillis());
        return new RunSummary(runId, "completed", exported, deleted);
    }

    private long[] archiveStream(LogStream stream, String runId, Instant deadline) {
        Instant cutoff = clock.instant();
        int part = 0;
        long exported = 0;
        long deleted = 0;

        while (clock.instant().isBefore(deadline)) {
            List<ArchiveRecord> batch = source.fetchArchivable(stream, cutoff, settings.batchSize());
            if (batch.isEmpty()) {
                break;
            }
            long deletedInBatch = 0;
            for (Map.Entry<LocalDate, List<ArchiveRecord>> day : groupByDay(batch).entrySet()) {
                part++;
                deletedInBatch += exportAndDelete(stream, runId, part, day.getKey(), day.getValue(), cutoff);
            }
            exported += batch.size();
            deleted += deletedInBatch;
            metrics.archived(stream, batch.size(), deletedInBatch);

            if (deletedInBatch == 0) {
                // Nothing could be removed; fetching again would return the same documents.
                log.warn("archive_stream_stalled stream={} runId={} batch={}", stream.tag(), runId, batch.size());
                break;
            }
            if (batch.size() < settings.batchSize()) {
                break;
            }
        }
        if (exported > 0) {
            log.info("archive_stream_done stream={} runId={} exported={} deleted={}", stream.tag(), runId,
                    exported, deleted);
        }
        return new long[] {exported, deleted};
    }

    private long exportAndDelete(LogStream stream, String runId, int part, LocalDate day,
                                 List<ArchiveRecord> records, Instant cutoff) {
        String partLabel = runId + "-" + String.format("%04d", part);
        String manifestId = stream.collection() + "/" + day + "/" + partLabel;
        String objectKey = stream.collection() + "/dt=" + day + "/" + partLabel + OBJECT_SUFFIX;

        List<String> lines = records.stream().map(ArchiveRecord::json).toList();
        StoredObject stored = store.write(objectKey, lines);
        boolean verified = stored.lineCount() == records.size() && store.verify(stored);

        ArchiveManifest manifest = new ArchiveManifest(manifestId, stream.collection(), runId, day.toString(),
                stored.objectKey(), records.size(), stored.bytes(), stored.sha256(),
                records.get(0).eventId(), records.get(records.size() - 1).eventId(),
                records.stream().map(ArchiveRecord::occurredAt).min(Instant::compareTo).orElse(null),
                records.stream().map(ArchiveRecord::occurredAt).max(Instant::compareTo).orElse(null),
                verified ? ArchiveStatus.EXPORTED : ArchiveStatus.FAILED, clock.instant(), null,
                verified ? null : "verification failed");
        manifests.save(manifest);
        if (!verified) {
            throw new IllegalStateException("archive object failed verification: " + stored.objectKey());
        }

        long removed;
        try {
            removed = deleteInChunks(stream, records.stream().map(ArchiveRecord::eventId).toList(), cutoff);
        } catch (RuntimeException e) {
            // The export is safe; the next run re-exports whatever is still in MongoDB.
            manifests.markFailed(manifestId, "delete failed: " + e.getMessage());
            throw e;
        }
        manifests.markDeleted(manifestId, clock.instant());
        return removed;
    }

    private long deleteInChunks(LogStream stream, List<String> ids, Instant cutoff) {
        long removed = 0;
        for (int from = 0; from < ids.size(); from += settings.deleteChunkSize()) {
            List<String> chunk = ids.subList(from, Math.min(from + settings.deleteChunkSize(), ids.size()));
            removed += source.deleteArchived(stream, chunk, cutoff);
            pause();
        }
        return removed;
    }

    /** Spreads delete load so the archiver never competes hard with ingestion. */
    private void pause() {
        if (settings.deletePause().isZero()) {
            return;
        }
        try {
            Thread.sleep(settings.deletePause().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("archive run interrupted", e);
        }
    }

    private static Map<LocalDate, List<ArchiveRecord>> groupByDay(List<ArchiveRecord> batch) {
        Map<LocalDate, List<ArchiveRecord>> byDay = new TreeMap<>();
        for (ArchiveRecord record : batch) {
            LocalDate day = LocalDate.ofInstant(record.occurredAt(), ZoneOffset.UTC);
            byDay.computeIfAbsent(day, d -> new ArrayList<>()).add(record);
        }
        return byDay;
    }

    public record Settings(int batchSize, int deleteChunkSize, Duration deletePause, Duration maxRun) {

        public Settings {
            if (batchSize < 1 || deleteChunkSize < 1) {
                throw new IllegalArgumentException("batchSize and deleteChunkSize must be positive");
            }
            Objects.requireNonNull(deletePause, "deletePause");
            Objects.requireNonNull(maxRun, "maxRun");
        }
    }

    public record RunSummary(String runId, String outcome, long exported, long deleted) {
    }
}
