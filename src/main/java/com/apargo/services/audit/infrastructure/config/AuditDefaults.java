package com.apargo.services.audit.infrastructure.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import com.apargo.platform.contract.event.EventEnvironments;
import com.apargo.platform.contract.event.EventSchemaVersion;
import com.apargo.platform.contract.event.EventTopics;
import com.apargo.services.audit.common.enums.ArchiveStoreType;

/**
 * The single source of every default. {@link AuditProperties} falls back to these when a key is
 * missing, and {@code application.yml} repeats the same values as {@code ${ENV_VAR:default}};
 * {@code AuditPropertiesDefaultsTest} fails if the two ever differ.
 */
public final class AuditDefaults {

    public static final String APPLICATION_NAME = "audit-service";
    public static final String ENVIRONMENT = EventEnvironments.DEVELOPMENT;

    // roles
    public static final boolean ROLE_CONSUMER = true;
    public static final boolean ROLE_API = true;
    public static final boolean ROLE_ARCHIVER = false;

    // topics (contract defaults)
    public static final String AUDIT_TOPIC = EventTopics.DEFAULT_AUDIT_TOPIC;
    public static final String ACCESS_TOPIC = EventTopics.DEFAULT_ACCESS_TOPIC;
    public static final String DLQ_SUFFIX = ".dlq";

    // consumer
    public static final String AUDIT_GROUP_ID = "audit-service.audit";
    public static final int AUDIT_CONCURRENCY = 3;
    public static final String ACCESS_GROUP_ID = "audit-service.access";
    public static final int ACCESS_CONCURRENCY = 2;
    public static final int MAX_EVENT_BYTES = 65_536;
    public static final Set<Integer> SUPPORTED_SCHEMA_VERSIONS = Set.of(EventSchemaVersion.AUDIT);
    public static final Duration MAX_FUTURE_SKEW = Duration.ofHours(24);
    public static final Duration RETRY_INITIAL_INTERVAL = Duration.ofSeconds(1);
    public static final double RETRY_MULTIPLIER = 2.0;
    public static final Duration RETRY_MAX_INTERVAL = Duration.ofSeconds(60);
    public static final int CIRCUIT_FAILURE_THRESHOLD = 5;
    public static final Duration CIRCUIT_OPEN_DURATION = Duration.ofSeconds(10);
    public static final Duration DEAD_LETTER_SEND_TIMEOUT = Duration.ofSeconds(15);

    // mongo
    public static final String MONGO_WRITE_URI = "mongodb://localhost:27017/?replicaSet=rs0";
    public static final String MONGO_DATABASE = "audit";
    public static final Duration MONGO_WRITE_TIMEOUT = Duration.ofSeconds(10);
    public static final Duration MONGO_QUERY_MAX_TIME = Duration.ofSeconds(3);
    public static final Duration MONGO_MAX_STALENESS = Duration.ofSeconds(90);
    public static final int MONGO_WRITE_POOL_SIZE = 20;
    public static final int MONGO_READ_POOL_SIZE = 30;
    public static final boolean MONGO_MANAGE_SCHEMA = true;
    public static final String MONGO_BLOCK_COMPRESSOR = "zstd";

    // retention
    public static final int AUDIT_HOT_DAYS = 30;
    public static final int ACCESS_HOT_DAYS = 30;

    // query
    public static final Duration QUERY_DEFAULT_RANGE = Duration.ofDays(7);
    public static final Duration QUERY_MAX_RANGE = Duration.ofDays(31);
    public static final int QUERY_DEFAULT_PAGE_SIZE = 20;
    public static final int QUERY_MAX_PAGE_SIZE = 100;
    public static final int QUERY_MAX_STATS_BUCKETS = 1000;

    // archiver
    public static final String ARCHIVER_CRON = "0 15 * * * *";
    public static final int ARCHIVER_BATCH_SIZE = 5000;
    public static final int ARCHIVER_DELETE_CHUNK_SIZE = 1000;
    public static final Duration ARCHIVER_DELETE_PAUSE = Duration.ofMillis(200);
    public static final Duration ARCHIVER_MAX_RUN = Duration.ofMinutes(45);
    public static final ArchiveStoreType ARCHIVE_STORE_TYPE = ArchiveStoreType.NONE;
    public static final String ARCHIVE_LOCAL_DIR = "./archive";

    // security: override the key in every shared environment (startup warns outside development)
    public static final String INTERNAL_API_KEY = "apargo-audit-internal-key";
    /** Any caller that presents the key. */
    public static final String ANY = "*";
    public static final Set<String> ALLOWED_CALLERS = Set.of(ANY);
    /** All browser origins. */
    public static final List<String> CORS_ALLOWED_ORIGINS = List.of(ANY);

    /**
     * Placeholders for annotations that read properties directly ({@code @KafkaListener},
     * {@code @Scheduled}, {@code @Value}), with the same defaults as above.
     */
    public static final class Placeholders {

        public static final String APPLICATION_NAME = "${spring.application.name:" + AuditDefaults.APPLICATION_NAME + "}";
        public static final String AUDIT_TOPIC = "${audit.topics.audit:" + AuditDefaults.AUDIT_TOPIC + "}";
        public static final String ACCESS_TOPIC = "${audit.topics.access:" + AuditDefaults.ACCESS_TOPIC + "}";
        public static final String AUDIT_GROUP_ID = "${audit.consumer.audit.group-id:" + AuditDefaults.AUDIT_GROUP_ID + "}";
        public static final String AUDIT_CONCURRENCY =
                "${audit.consumer.audit.concurrency:" + AuditDefaults.AUDIT_CONCURRENCY + "}";
        public static final String ACCESS_GROUP_ID = "${audit.consumer.access.group-id:" + AuditDefaults.ACCESS_GROUP_ID + "}";
        public static final String ACCESS_CONCURRENCY =
                "${audit.consumer.access.concurrency:" + AuditDefaults.ACCESS_CONCURRENCY + "}";
        public static final String ARCHIVER_CRON = "${audit.archiver.cron:" + AuditDefaults.ARCHIVER_CRON + "}";

        private Placeholders() {
        }
    }

    private AuditDefaults() {
    }
}
