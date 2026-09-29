package com.apargo.services.audit.common.constant;

/**
 * Headers added to a dead-lettered record. The original key, value and headers are kept
 * unchanged, so a record can be replayed to its source topic as-is.
 */
public final class DeadLetterHeaders {

    public static final String REASON = "x-dlq-reason";
    public static final String DETAIL = "x-dlq-detail";
    public static final String STREAM = "x-dlq-stream";
    public static final String ORIGINAL_TOPIC = "x-dlq-original-topic";
    public static final String ORIGINAL_PARTITION = "x-dlq-original-partition";
    public static final String ORIGINAL_OFFSET = "x-dlq-original-offset";
    public static final String FAILED_AT = "x-dlq-failed-at";
    public static final String SOURCE_APP = "x-dlq-source-app";

    /** Detail text is capped so a header never carries a large payload. */
    public static final int MAX_DETAIL_LENGTH = 1000;

    private DeadLetterHeaders() {
    }
}
