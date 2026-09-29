package com.apargo.services.audit.application.port.out;

import java.util.List;

/**
 * Cold storage. Pluggable: a disabled store means the archiver never exports or deletes, so data
 * stays in MongoDB until a real store is configured.
 */
public interface ArchiveStore {

    boolean isEnabled();

    /** Human-readable target, for logs (never contains credentials). */
    String describe();

    /** Writes the lines as one object and returns what was stored. */
    StoredObject write(String objectKey, List<String> lines);

    /** True only if the stored object is complete and matches {@code object}. */
    boolean verify(StoredObject object);

    record StoredObject(String objectKey, long bytes, String sha256, int lineCount) {
    }
}
