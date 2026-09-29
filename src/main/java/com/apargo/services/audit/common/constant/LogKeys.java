package com.apargo.services.audit.common.constant;

/** MDC keys. Shared by the HTTP filters, the Kafka listeners and the log pattern. */
public final class LogKeys {

    public static final String REQUEST_ID = "requestId";
    public static final String TRACE_ID = "traceId";
    public static final String ORG_ID = "orgId";
    public static final String PROJECT_ID = "projectId";
    public static final String USER_ID = "userId";
    public static final String CALLER_SERVICE = "callerService";
    public static final String STREAM = "stream";
    public static final String JOB_NAME = "jobName";

    private LogKeys() {
    }
}
