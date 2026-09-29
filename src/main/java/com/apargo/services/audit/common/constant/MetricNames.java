package com.apargo.services.audit.common.constant;

/** Micrometer meter and tag names. */
public final class MetricNames {

    public static final String INGEST_RECORDS = "audit.ingest.records";
    public static final String INGEST_BATCH_SIZE = "audit.ingest.batch.size";
    public static final String INGEST_BATCH_DURATION = "audit.ingest.batch.duration";
    public static final String INGEST_DEAD_LETTERS = "audit.ingest.dead.letters";
    public static final String INGEST_CONTRACT_VIOLATIONS = "audit.ingest.contract.violations";
    public static final String INGEST_UNKNOWN_FIELDS = "audit.ingest.unknown.fields";
    public static final String CONSUMER_CIRCUIT_OPEN = "audit.consumer.circuit.open";
    public static final String ARCHIVE_RECORDS = "audit.archive.records";
    public static final String ARCHIVE_RUNS = "audit.archive.runs";

    public static final String TAG_STREAM = "stream";
    public static final String TAG_OUTCOME = "outcome";
    public static final String TAG_REASON = "reason";
    public static final String TAG_SOURCE = "source";
    public static final String TAG_RULE = "rule";
    public static final String TAG_TYPE = "type";
    public static final String TAG_FIELD = "field";
    public static final String TAG_ACTION = "action";

    public static final String OUTCOME_STORED = "stored";
    public static final String OUTCOME_DUPLICATE = "duplicate";
    public static final String OUTCOME_DEAD_LETTERED = "dead_lettered";
    public static final String ACTION_EXPORTED = "exported";
    public static final String ACTION_DELETED = "deleted";
    public static final String UNKNOWN_TAG_VALUE = "unknown";

    private MetricNames() {
    }
}
