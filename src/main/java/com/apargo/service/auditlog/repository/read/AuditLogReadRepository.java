package com.apargo.service.auditlog.repository.read;

import com.apargo.service.auditlog.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Read-only side of the CQRS split, for simple derived-query needs (findById).
 * Anything involving dynamic filters goes through
 * {@link AuditLogSearchRepository} instead, which builds Criteria at runtime.
 */
public interface AuditLogReadRepository extends MongoRepository<AuditLog, String> {
}
