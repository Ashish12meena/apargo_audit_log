package com.apargo.services.audit.application.port.out;

import java.time.Instant;

import com.apargo.services.audit.domain.model.ArchiveManifest;

/** Records every exported archive file, so deletes are always backed by a verified export. */
public interface ArchiveManifestStore {

    void save(ArchiveManifest manifest);

    void markDeleted(String manifestId, Instant deletedAt);

    void markFailed(String manifestId, String error);
}
