package com.apargo.service.auditlog.repository.write;

import com.apargo.service.auditlog.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Write-only side of the CQRS split. Deliberately kept to just what
 * MongoRepository gives for free (insert/save) — no @Query/filter methods
 * here. Query methods belong in {@code repository.read}. This separation
 * matters for a write-heavy service: it keeps the hot insert path free of
 * any temptation to add expensive lookups on the same repository, and makes
 * it trivial later to point this repository at a dedicated primary-only
 * MongoTemplate while the read side points at a secondaryPreferred one.
 */
public interface AuditLogWriteRepository extends MongoRepository<AuditLog, String> {

    boolean existsByEventId(String eventId);
}
