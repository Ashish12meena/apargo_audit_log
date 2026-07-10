package com.apargo.service.auditlog.repository.read;

import com.apargo.service.auditlog.dto.request.AuditSearchRequest;
import com.apargo.service.auditlog.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Dynamic filter contract for GET /api/v1/audit-logs. */
public interface AuditLogSearchRepository {

    Page<AuditLog> search(AuditSearchRequest filters, Pageable pageable);
}
