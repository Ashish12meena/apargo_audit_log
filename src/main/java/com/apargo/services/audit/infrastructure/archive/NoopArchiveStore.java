package com.apargo.services.audit.infrastructure.archive;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.port.out.ArchiveStore;

/** Default store: archiving is off, so nothing is ever exported or deleted. */
@Component
@ConditionalOnProperty(prefix = "audit.archiver.store", name = "type", havingValue = "NONE", matchIfMissing = true)
public class NoopArchiveStore implements ArchiveStore {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public String describe() {
        return "none";
    }

    @Override
    public StoredObject write(String objectKey, List<String> lines) {
        throw new UnsupportedOperationException("No archive store is configured");
    }

    @Override
    public boolean verify(StoredObject object) {
        return false;
    }
}
