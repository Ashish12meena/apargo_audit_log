package com.apargo.services.audit.infrastructure.archive;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.port.out.ArchiveStore;
import com.apargo.services.audit.infrastructure.config.AuditProperties;

/**
 * Writes archive objects under a local directory. For development and tests only: local disk
 * is not durable cold storage. Writes are atomic (temp file, then move).
 */
@Component
@ConditionalOnProperty(prefix = "audit.archiver.store", name = "type", havingValue = "LOCAL")
public class LocalFileArchiveStore implements ArchiveStore {

    private final Path root;

    public LocalFileArchiveStore(AuditProperties properties) {
        String dir = properties.archiver().store().localDir();
        if (dir == null || dir.isBlank()) {
            throw new IllegalArgumentException("audit.archiver.store.local-dir is required for the LOCAL store");
        }
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String describe() {
        return "local:" + root;
    }

    @Override
    public StoredObject write(String objectKey, List<String> lines) {
        Path target = resolve(objectKey);
        byte[] content = NdjsonGzipCodec.encode(lines);
        try {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            Files.write(temp, content);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write archive object " + objectKey, e);
        }
        return new StoredObject(objectKey, content.length, NdjsonGzipCodec.sha256(content), lines.size());
    }

    @Override
    public boolean verify(StoredObject object) {
        try {
            byte[] stored = Files.readAllBytes(resolve(object.objectKey()));
            return stored.length == object.bytes()
                    && NdjsonGzipCodec.sha256(stored).equals(object.sha256())
                    && NdjsonGzipCodec.countLines(stored) == object.lineCount();
        } catch (IOException e) {
            return false;
        }
    }

    /** Keeps every object inside the root directory. */
    private Path resolve(String objectKey) {
        Path path = root.resolve(objectKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Object key escapes the archive directory: " + objectKey);
        }
        return path;
    }
}
