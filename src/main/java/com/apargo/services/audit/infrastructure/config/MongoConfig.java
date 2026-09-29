package com.apargo.services.audit.infrastructure.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Two MongoDB clients with separate connection pools, so read traffic can never starve ingestion:
 * <ul>
 *   <li><b>write</b>: primary, {@code w:majority, j:true} with a write timeout. Offsets are committed
 *       only for data that survives a failover.</li>
 *   <li><b>read</b>: {@code secondaryPreferred} with bounded staleness; audit reads tolerate a few
 *       seconds of lag.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class MongoConfig {

    public static final String WRITE_CLIENT = "auditWriteMongoClient";
    public static final String READ_CLIENT = "auditReadMongoClient";
    public static final String WRITE_DATABASE = "auditWriteDatabase";
    public static final String READ_DATABASE = "auditReadDatabase";

    @Bean(name = WRITE_CLIENT, destroyMethod = "close")
    public MongoClient auditWriteMongoClient(AuditProperties properties,
                                             @Value(AuditDefaults.Placeholders.APPLICATION_NAME) String applicationName) {
        AuditProperties.Mongo mongo = properties.mongo();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongo.writeUri()))
                .applicationName(applicationName + "-writer")
                .readPreference(ReadPreference.primary())
                .writeConcern(WriteConcern.MAJORITY.withJournal(true)
                        .withWTimeout(mongo.writeTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .retryWrites(true)
                .applyToConnectionPoolSettings(pool -> pool.maxSize(mongo.writePoolSize()))
                .build();
        return MongoClients.create(settings);
    }

    @Bean(name = READ_CLIENT, destroyMethod = "close")
    public MongoClient auditReadMongoClient(AuditProperties properties,
                                            @Value(AuditDefaults.Placeholders.APPLICATION_NAME) String applicationName) {
        AuditProperties.Mongo mongo = properties.mongo();
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongo.effectiveReadUri()))
                .applicationName(applicationName + "-reader")
                .readPreference(ReadPreference.secondaryPreferred(mongo.maxStaleness().toMillis(), TimeUnit.MILLISECONDS))
                .retryReads(true)
                .applyToConnectionPoolSettings(pool -> pool.maxSize(mongo.readPoolSize()))
                .build();
        return MongoClients.create(settings);
    }

    @Bean(name = WRITE_DATABASE)
    public MongoDatabase auditWriteDatabase(@Qualifier(WRITE_CLIENT) MongoClient client, AuditProperties properties) {
        return client.getDatabase(properties.mongo().database());
    }

    @Bean(name = READ_DATABASE)
    public MongoDatabase auditReadDatabase(@Qualifier(READ_CLIENT) MongoClient client, AuditProperties properties) {
        return client.getDatabase(properties.mongo().database());
    }
}
