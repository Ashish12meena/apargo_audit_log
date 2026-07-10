package com.apargo.service.auditlog.config;

import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Write-heavy service: this app is expected to take far more inserts than
 * queries, so writes and reads get separate MongoTemplate beans.
 * <p>
 * - {@code mongoTemplate} (primary) is used implicitly by the write
 *   repository / Spring Data — always talks to the primary node so inserts
 *   are immediately durable and never routed to a lagging secondary.
 * - {@code readMongoTemplate} is used explicitly by AuditLogSearchRepositoryImpl
 *   and prefers secondaries (falls back to primary if none are available),
 *   so heavy filter/pagination queries on the audit_log collection don't
 *   compete with the write path for primary node resources.
 * <p>
 * If running against a single-node Mongo (e.g. local dev / this Atlas free
 * tier connection string), both beans behave identically since there's no
 * secondary to route to — this only pays off once the replica set has
 * secondaries.
 */
@Configuration
public class MongoReadWriteConfig {

    @Value("${spring.data.mongodb.database:audit}")
    private String database;

    @Primary
    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, database);
    }

    @Bean
    public MongoTemplate readMongoTemplate(MongoClient mongoClient) {
        MongoTemplate template = new MongoTemplate(mongoClient, database);
        template.setReadPreference(ReadPreference.secondaryPreferred());
        return template;
    }
}
