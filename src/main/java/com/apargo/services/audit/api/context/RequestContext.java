package com.apargo.services.audit.api.context;

/**
 * Who a request acts for, taken only from headers (API standard §1).
 *
 * @param orgId         {@code X-Org-Id}; always set on public routes, optional on internal ones
 * @param projectId     {@code X-Project-Id}; when set, results are limited to that project
 * @param userId        {@code X-User-Id}, for logs
 * @param callerService {@code X-Internal-Caller}, internal routes only
 * @param internal      true on {@code /internal/**}
 */
public record RequestContext(Long orgId, Long projectId, String userId, String callerService, boolean internal) {
}
