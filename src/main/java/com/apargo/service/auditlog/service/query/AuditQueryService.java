package com.apargo.service.auditlog.service.query;

import com.apargo.service.auditlog.dto.request.AuditSearchRequest;
import com.apargo.service.auditlog.dto.response.AuditEventResponse;
import com.apargo.service.auditlog.dto.response.PagedResponse;
import com.apargo.service.auditlog.exception.AuditNotFoundException;
import com.apargo.service.auditlog.mapper.AuditLogMapper;
import com.apargo.service.auditlog.model.AuditLog;
import com.apargo.service.auditlog.repository.read.AuditLogReadRepository;
import com.apargo.service.auditlog.repository.read.AuditLogSearchRepository;
import com.apargo.service.auditlog.util.AuditPageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditQueryService {

    private final AuditLogReadRepository readRepository;
    private final AuditLogSearchRepository searchRepository;
    private final AuditLogMapper mapper;

    public PagedResponse<AuditEventResponse> search(AuditSearchRequest filters) {
        Pageable pageable = AuditPageUtil.build(filters.getPage(), filters.getLimit(),
                filters.getSortBy(), filters.getSortOrder());

        Page<AuditLog> page = searchRepository.search(filters, pageable);

        log.debug("audit_search orgId={} module={} action={} status={} resultCount={} totalItems={}",
                filters.getOrganizationId(), filters.getModule(), filters.getAction(),
                filters.getStatus(), page.getNumberOfElements(), page.getTotalElements());

        return PagedResponse.<AuditEventResponse>builder()
                .items(page.getContent().stream().map(mapper::toResponse).toList())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(pageable.getPageNumber() + 1)
                .limit(pageable.getPageSize())
                .build();
    }

    public AuditEventResponse getById(String id) {
        AuditLog doc = readRepository.findById(id)
                .orElseThrow(() -> new AuditNotFoundException(id));
        return mapper.toResponse(doc);
    }
}
