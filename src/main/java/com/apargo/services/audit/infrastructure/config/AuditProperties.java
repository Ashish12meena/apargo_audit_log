package com.apargo.services.audit.infrastructure.config;

import static com.apargo.services.audit.infrastructure.config.AuditDefaults.*;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.apargo.platform.contract.event.EventEnvironments;
import com.apargo.services.audit.common.enums.ArchiveStoreType;
import com.apargo.services.audit.common.enums.LogStream;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Every {@code audit.*} setting. A missing key (or a whole missing section) falls back to
 * {@link AuditDefaults}, the same values {@code application.yml} uses as {@code ${ENV_VAR:default}};
 * the result is then validated at startup. Numbers and flags are boxed so "missing" can be told
 * apart from a configured value.
 */
@Validated
@ConfigurationProperties(prefix = "audit")
public record AuditProperties(
        String environment,
        @Valid Roles roles,
        @Valid Topics topics,
        @Valid Consumer consumer,
        @Valid Mongo mongo,
        @Valid Retention retention,
        @Valid Query query,
        @Valid Archiver archiver,
        @Valid Security security) {

    public AuditProperties {
        environment = text(environment, ENVIRONMENT);
        if (!EventEnvironments.isValid(environment)) {
            throw new IllegalArgumentException("audit.environment must be one of " + EventEnvironments.ALL);
        }
        roles = value(roles, new Roles(null, null, null));
        topics = value(topics, new Topics(null, null, null));
        consumer = value(consumer, new Consumer(null, null, null, null, null, null, null, null));
        mongo = value(mongo, new Mongo(null, null, null, null, null, null, null, null, null, null));
        retention = value(retention, new Retention(null, null));
        query = value(query, new Query(null, null, null, null, null));
        archiver = value(archiver, new Archiver(null, null, null, null, null, null));
        security = value(security, new Security(null, null, null));
    }

    /** All defaults, as if no {@code audit.*} key were configured. */
    public static AuditProperties defaults() {
        return new AuditProperties(null, null, null, null, null, null, null, null, null);
    }

    /** Which parts of the service this instance runs. */
    public record Roles(Boolean consumer, Boolean api, Boolean archiver) {

        public Roles {
            consumer = value(consumer, ROLE_CONSUMER);
            api = value(api, ROLE_API);
            archiver = value(archiver, ROLE_ARCHIVER);
        }
    }

    public record Topics(@NotBlank String audit, @NotBlank String access, @NotBlank String dlqSuffix) {

        public Topics {
            audit = text(audit, AUDIT_TOPIC);
            access = text(access, ACCESS_TOPIC);
            dlqSuffix = text(dlqSuffix, DLQ_SUFFIX);
        }

        public String topic(LogStream stream) {
            return switch (stream) {
                case AUDIT -> audit;
                case ACCESS -> access;
            };
        }

        public String deadLetterTopic(LogStream stream) {
            return topic(stream) + dlqSuffix;
        }
    }

    public record Consumer(
            @Valid StreamConsumer audit,
            @Valid StreamConsumer access,
            @Min(1024) Integer maxEventBytes,
            @NotEmpty Set<Integer> supportedSchemaVersions,
            Duration maxFutureSkew,
            @Valid Retry retry,
            @Valid CircuitBreaker circuitBreaker,
            Duration deadLetterSendTimeout) {

        public Consumer {
            audit = StreamConsumer.withDefaults(audit, AUDIT_GROUP_ID, AUDIT_CONCURRENCY);
            access = StreamConsumer.withDefaults(access, ACCESS_GROUP_ID, ACCESS_CONCURRENCY);
            maxEventBytes = value(maxEventBytes, MAX_EVENT_BYTES);
            supportedSchemaVersions = supportedSchemaVersions == null || supportedSchemaVersions.isEmpty()
                    ? SUPPORTED_SCHEMA_VERSIONS : Set.copyOf(supportedSchemaVersions);
            maxFutureSkew = value(maxFutureSkew, MAX_FUTURE_SKEW);
            retry = value(retry, new Retry(null, null, null));
            circuitBreaker = value(circuitBreaker, new CircuitBreaker(null, null));
            deadLetterSendTimeout = value(deadLetterSendTimeout, DEAD_LETTER_SEND_TIMEOUT);
        }
    }

    /** Consumer group and thread count of one stream; each stream has its own defaults. */
    public record StreamConsumer(@NotBlank String groupId, @Min(1) @Max(64) Integer concurrency) {

        static StreamConsumer withDefaults(StreamConsumer configured, String groupId, int concurrency) {
            return configured == null
                    ? new StreamConsumer(groupId, concurrency)
                    : new StreamConsumer(text(configured.groupId(), groupId), value(configured.concurrency(), concurrency));
        }
    }

    public record Retry(Duration initialInterval, @DecimalMin("1.0") Double multiplier, Duration maxInterval) {

        public Retry {
            initialInterval = value(initialInterval, RETRY_INITIAL_INTERVAL);
            multiplier = value(multiplier, RETRY_MULTIPLIER);
            maxInterval = value(maxInterval, RETRY_MAX_INTERVAL);
        }
    }

    public record CircuitBreaker(@Min(1) Integer failureThreshold, Duration openDuration) {

        public CircuitBreaker {
            failureThreshold = value(failureThreshold, CIRCUIT_FAILURE_THRESHOLD);
            openDuration = value(openDuration, CIRCUIT_OPEN_DURATION);
        }
    }

    public record Mongo(
            @NotBlank String writeUri,
            String readUri,
            @NotBlank String database,
            Duration writeTimeout,
            Duration queryMaxTime,
            Duration maxStaleness,
            @Min(1) Integer writePoolSize,
            @Min(1) Integer readPoolSize,
            Boolean manageSchema,
            @NotBlank String blockCompressor) {

        /** Minimum MongoDB accepts for maxStalenessSeconds. */
        private static final Duration MIN_STALENESS = Duration.ofSeconds(90);

        public Mongo {
            writeUri = text(writeUri, MONGO_WRITE_URI);
            readUri = text(readUri, writeUri);
            database = text(database, MONGO_DATABASE);
            writeTimeout = value(writeTimeout, MONGO_WRITE_TIMEOUT);
            queryMaxTime = value(queryMaxTime, MONGO_QUERY_MAX_TIME);
            maxStaleness = value(maxStaleness, MONGO_MAX_STALENESS);
            writePoolSize = value(writePoolSize, MONGO_WRITE_POOL_SIZE);
            readPoolSize = value(readPoolSize, MONGO_READ_POOL_SIZE);
            manageSchema = value(manageSchema, MONGO_MANAGE_SCHEMA);
            blockCompressor = text(blockCompressor, MONGO_BLOCK_COMPRESSOR);
            if (maxStaleness.compareTo(MIN_STALENESS) < 0) {
                throw new IllegalArgumentException("audit.mongo.max-staleness must be at least 90s");
            }
        }

        /** Reads use their own URI (e.g. a read-only user); unset, it is the write URI. */
        public String effectiveReadUri() {
            return readUri;
        }

        public boolean compressionEnabled() {
            return !"none".equalsIgnoreCase(blockCompressor);
        }
    }

    public record Retention(@Min(1) Integer auditHotDays, @Min(1) Integer accessHotDays) {

        public Retention {
            auditHotDays = value(auditHotDays, AUDIT_HOT_DAYS);
            accessHotDays = value(accessHotDays, ACCESS_HOT_DAYS);
        }
    }

    public record Query(
            Duration defaultRange,
            Duration maxRange,
            @Min(1) Integer defaultPageSize,
            @Min(1) @Max(100) Integer maxPageSize,
            @Min(1) Integer maxStatsBuckets) {

        public Query {
            defaultRange = value(defaultRange, QUERY_DEFAULT_RANGE);
            maxRange = value(maxRange, QUERY_MAX_RANGE);
            defaultPageSize = value(defaultPageSize, QUERY_DEFAULT_PAGE_SIZE);
            maxPageSize = value(maxPageSize, QUERY_MAX_PAGE_SIZE);
            maxStatsBuckets = value(maxStatsBuckets, QUERY_MAX_STATS_BUCKETS);
        }
    }

    public record Archiver(
            @NotBlank String cron,
            @Min(1) Integer batchSize,
            @Min(1) Integer deleteChunkSize,
            Duration deletePause,
            Duration maxRun,
            @Valid Store store) {

        public Archiver {
            cron = text(cron, ARCHIVER_CRON);
            batchSize = value(batchSize, ARCHIVER_BATCH_SIZE);
            deleteChunkSize = value(deleteChunkSize, ARCHIVER_DELETE_CHUNK_SIZE);
            deletePause = value(deletePause, ARCHIVER_DELETE_PAUSE);
            maxRun = value(maxRun, ARCHIVER_MAX_RUN);
            store = value(store, new Store(null, null));
        }
    }

    public record Store(ArchiveStoreType type, String localDir) {

        public Store {
            type = value(type, ARCHIVE_STORE_TYPE);
            localDir = text(localDir, ARCHIVE_LOCAL_DIR);
        }
    }

    /**
     * Internal API key (blank closes {@code /internal/**}), caller allow-list ({@code *} = any caller
     * with the key) and CORS origins ({@code *} = all, empty = CORS off).
     */
    public record Security(String internalApiKey, Set<String> allowedCallers, List<String> corsAllowedOrigins) {

        public Security {
            internalApiKey = value(internalApiKey, INTERNAL_API_KEY).trim();
            allowedCallers = allowedCallers == null ? ALLOWED_CALLERS : allowedCallers.stream()
                    .filter(c -> c != null && !c.isBlank()).map(String::trim).collect(Collectors.toUnmodifiableSet());
            corsAllowedOrigins = corsAllowedOrigins == null ? CORS_ALLOWED_ORIGINS : corsAllowedOrigins.stream()
                    .filter(o -> o != null && !o.isBlank()).map(String::trim).toList();
        }

        public boolean internalApiEnabled() {
            return !internalApiKey.isBlank();
        }

        public boolean anyCallerAllowed() {
            return allowedCallers.isEmpty() || allowedCallers.contains(ANY);
        }

        public boolean usesDefaultInternalApiKey() {
            return INTERNAL_API_KEY.equals(internalApiKey);
        }
    }

    private static <T> T value(T configured, T fallback) {
        return configured != null ? configured : fallback;
    }

    /** Blank text counts as missing for settings that cannot be empty. */
    private static String text(String configured, String fallback) {
        return configured != null && !configured.isBlank() ? configured.trim() : fallback;
    }
}
