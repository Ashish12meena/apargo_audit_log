package com.apargo.services.audit.common.constant;

import java.util.List;

/** Collection names and the classpath resources that define them (validator + indexes). */
public final class MongoCollections {

    public static final String AUDIT_LOGS = "audit_logs";
    public static final String ACCESS_LOGS = "access_logs";
    public static final String ARCHIVE_MANIFESTS = "archive_manifests";
    public static final String JOB_LOCKS = "job_locks";

    /** Definitions applied by MongoSchemaInitializer, in this order. */
    public static final List<String> DEFINITION_RESOURCES = List.of(
            "db/mongo/audit_logs.json",
            "db/mongo/access_logs.json",
            "db/mongo/archive_manifests.json");

    private MongoCollections() {
    }
}
