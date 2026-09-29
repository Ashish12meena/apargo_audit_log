package com.apargo.services.audit.common.constant;

/** HTTP header names from the platform API standard. */
public final class ApiHeaders {

    public static final String ORG_ID = "X-Org-Id";
    public static final String PROJECT_ID = "X-Project-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String REQUEST_ID = "X-Request-Id";
    public static final String INTERNAL_API_KEY = "X-Internal-Api-Key";
    public static final String INTERNAL_CALLER = "X-Internal-Caller";
    /** W3C trace context; honoured only on /internal/** (public requests always get a new trace id). */
    public static final String TRACEPARENT = "traceparent";
    public static final String RETRY_AFTER = "Retry-After";

    private ApiHeaders() {
    }
}
