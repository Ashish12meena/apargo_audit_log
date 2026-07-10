package com.apargo.service.auditlog.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a Mongo-safe {@link Pageable} from client-supplied page/limit/sortBy/sortOrder.
 * Centralized so every query entry point (REST search, future admin tools)
 * enforces the same limit cap and sortBy whitelist.
 */
public final class AuditPageUtil {

    private AuditPageUtil() {
    }

    public static Pageable build(Integer page, Integer limit, String sortBy, String sortOrder) {
        int safePage = (page == null || page < 1) ? AuditConstants.DEFAULT_PAGE : page;
        int safeLimit = (limit == null || limit < 1)
                ? AuditConstants.DEFAULT_LIMIT
                : Math.min(limit, AuditConstants.MAX_LIMIT);

        String safeSortBy = (sortBy != null && AuditConstants.SORTABLE_FIELDS.contains(sortBy))
                ? sortBy
                : AuditConstants.DEFAULT_SORT_BY;

        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // page is 1-indexed on the API, Spring Data Pageable is 0-indexed
        return PageRequest.of(safePage - 1, safeLimit, Sort.by(direction, safeSortBy));
    }
}
