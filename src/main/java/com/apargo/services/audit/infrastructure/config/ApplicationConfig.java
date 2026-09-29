package com.apargo.services.audit.infrastructure.config;

import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import com.apargo.platform.contract.event.EventEnvironments;
import com.apargo.services.audit.application.archive.ArchiveService;
import com.apargo.services.audit.application.ingest.EventDecoder;
import com.apargo.services.audit.application.ingest.IngestBatchService;
import com.apargo.services.audit.application.port.out.AccessLogReader;
import com.apargo.services.audit.application.port.out.ArchivableLogStore;
import com.apargo.services.audit.application.port.out.ArchiveManifestStore;
import com.apargo.services.audit.application.port.out.ArchiveStore;
import com.apargo.services.audit.application.port.out.AuditLogReader;
import com.apargo.services.audit.application.port.out.AuditMetrics;
import com.apargo.services.audit.application.port.out.DeadLetterPublisher;
import com.apargo.services.audit.application.port.out.LogWriter;
import com.apargo.services.audit.application.query.AccessLogQueryService;
import com.apargo.services.audit.application.query.AuditLogQueryService;
import com.apargo.services.audit.application.query.AuditStatsService;
import com.apargo.services.audit.application.query.QuerySettings;
import com.apargo.services.audit.domain.mapping.AccessLogFactory;
import com.apargo.services.audit.domain.mapping.AuditLogFactory;
import com.apargo.services.audit.domain.policy.RetentionPolicy;
import com.apargo.services.audit.domain.validation.AcceptancePolicy;
import com.apargo.services.audit.domain.validation.AccessEventRules;
import com.apargo.services.audit.domain.validation.AuditEventRules;

/**
 * Wires the framework-free domain and application classes from {@link AuditProperties}. Beans of
 * a role that is switched off are not created.
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationConfig {

    private static final Logger log = LoggerFactory.getLogger(ApplicationConfig.class);
    private static final String ROLES = "audit.roles";
    private static final String KAFKA_BOOTSTRAP_SERVERS = "spring.kafka.bootstrap-servers";
    private static final String LOCALHOST = "localhost";

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RetentionPolicy retentionPolicy(AuditProperties properties) {
        return new RetentionPolicy(Duration.ofDays(properties.retention().auditHotDays()),
                Duration.ofDays(properties.retention().accessHotDays()));
    }

    // ---- ingestion -----------------------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(prefix = ROLES, name = "consumer", havingValue = "true", matchIfMissing = true)
    public AcceptancePolicy acceptancePolicy(AuditProperties properties) {
        AuditProperties.Consumer consumer = properties.consumer();
        return new AcceptancePolicy(properties.environment(), consumer.supportedSchemaVersions(),
                consumer.maxFutureSkew());
    }

    @Bean
    @ConditionalOnProperty(prefix = ROLES, name = "consumer", havingValue = "true", matchIfMissing = true)
    public IngestBatchService ingestBatchService(AcceptancePolicy policy, RetentionPolicy retentionPolicy,
                                                 LogWriter writer, DeadLetterPublisher deadLetterPublisher,
                                                 AuditMetrics metrics, Clock clock, AuditProperties properties) {
        return new IngestBatchService(
                new EventDecoder(metrics::unknownField),
                policy,
                new AuditEventRules(policy),
                new AccessEventRules(policy),
                new AuditLogFactory(retentionPolicy),
                new AccessLogFactory(retentionPolicy),
                writer,
                deadLetterPublisher,
                metrics,
                clock,
                properties.consumer().maxEventBytes());
    }

    // ---- read API ------------------------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(prefix = ROLES, name = "api", havingValue = "true", matchIfMissing = true)
    public QuerySettings querySettings(AuditProperties properties) {
        AuditProperties.Query query = properties.query();
        return new QuerySettings(query.defaultRange(), query.maxRange(), query.defaultPageSize(),
                query.maxPageSize(), query.maxStatsBuckets());
    }

    @Bean
    @ConditionalOnProperty(prefix = ROLES, name = "api", havingValue = "true", matchIfMissing = true)
    public AuditLogQueryService auditLogQueryService(AuditLogReader reader, QuerySettings settings, Clock clock) {
        return new AuditLogQueryService(reader, settings, clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = ROLES, name = "api", havingValue = "true", matchIfMissing = true)
    public AccessLogQueryService accessLogQueryService(AccessLogReader reader, QuerySettings settings, Clock clock) {
        return new AccessLogQueryService(reader, settings, clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = ROLES, name = "api", havingValue = "true", matchIfMissing = true)
    public AuditStatsService auditStatsService(AuditLogReader reader, QuerySettings settings, Clock clock) {
        return new AuditStatsService(reader, settings, clock);
    }

    // ---- archiving -----------------------------------------------------------------------------

    @Bean
    @ConditionalOnProperty(prefix = ROLES, name = "archiver", havingValue = "true")
    public ArchiveService archiveService(ArchivableLogStore source, ArchiveStore store,
                                         ArchiveManifestStore manifests, AuditMetrics metrics, Clock clock,
                                         AuditProperties properties) {
        AuditProperties.Archiver archiver = properties.archiver();
        return new ArchiveService(source, store, manifests, metrics, clock, new ArchiveService.Settings(
                archiver.batchSize(), archiver.deleteChunkSize(), archiver.deletePause(), archiver.maxRun()));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reportStartup(ApplicationReadyEvent event) {
        AuditProperties properties = event.getApplicationContext().getBean(AuditProperties.class);
        AuditProperties.Roles roles = properties.roles();
        log.info("audit_service_ready environment={} consumer={} api={} archiver={} auditTopic={} accessTopic={} "
                        + "hotDays={}/{} archiveStore={} internalApi={}",
                properties.environment(), roles.consumer(), roles.api(), roles.archiver(),
                properties.topics().audit(), properties.topics().access(),
                properties.retention().auditHotDays(), properties.retention().accessHotDays(),
                properties.archiver().store().type(),
                properties.security().internalApiEnabled() ? "enabled" : "disabled (no key configured)");
        warnAboutDefaultsOutsideDevelopment(properties, event.getApplicationContext().getEnvironment());
    }

    /** Every setting has a default so the service always starts; outside development, say which ones are still local. */
    private static void warnAboutDefaultsOutsideDevelopment(AuditProperties properties, Environment environment) {
        if (EventEnvironments.DEVELOPMENT.equals(properties.environment())) {
            return;
        }
        String brokers = environment.getProperty(KAFKA_BOOTSTRAP_SERVERS, "");
        if (brokers.contains(LOCALHOST)) {
            log.warn("audit_config_warning setting={} reason=localhost_default environment={}",
                    KAFKA_BOOTSTRAP_SERVERS, properties.environment());
        }
        if (properties.mongo().writeUri().contains(LOCALHOST)) {
            log.warn("audit_config_warning setting=audit.mongo.write-uri reason=localhost_default environment={}",
                    properties.environment());
        }
        if (!properties.security().internalApiEnabled()) {
            log.warn("audit_config_warning setting=audit.security.internal-api-key reason=blank_internal_routes_closed "
                    + "environment={}", properties.environment());
        } else if (properties.security().usesDefaultInternalApiKey()) {
            log.warn("audit_config_warning setting=audit.security.internal-api-key reason=default_key environment={}",
                    properties.environment());
        }
    }
}
