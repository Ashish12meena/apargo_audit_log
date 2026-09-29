package com.apargo.services.audit.common.constant;

/** Route prefixes. Public routes are tenant-scoped; internal routes need the internal API key. */
public final class ApiPaths {

    public static final String PUBLIC_PREFIX = "/api/";
    public static final String INTERNAL_PREFIX = "/internal/";

    public static final String AUDIT_LOGS = "/api/v1/audit-logs";
    public static final String ACCESS_LOGS = "/api/v1/access-logs";
    public static final String INTERNAL_AUDIT_LOGS = "/internal/v1/audit-logs";
    public static final String INTERNAL_ACCESS_LOGS = "/internal/v1/access-logs";

    public static final String BY_EVENT_ID = "/{eventId}";
    public static final String STATS = "/stats";
    public static final String BY_TRACE_ID = "/trace/{traceId}";

    private ApiPaths() {
    }

    public static boolean isInternal(String path) {
        return path != null && path.startsWith(INTERNAL_PREFIX);
    }

    public static boolean isPublic(String path) {
        return path != null && path.startsWith(PUBLIC_PREFIX);
    }
}
