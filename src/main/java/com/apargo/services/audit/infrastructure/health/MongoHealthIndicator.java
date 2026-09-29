package com.apargo.services.audit.infrastructure.health;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.infrastructure.config.MongoConfig;
import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;

/** Pings both MongoDB clients. Part of the readiness group ({@code auditMongo}). */
@Component("auditMongoHealthIndicator")
public class MongoHealthIndicator implements HealthIndicator {

    private static final Document PING = new Document("ping", 1);

    private final MongoDatabase writeDatabase;
    private final MongoDatabase readDatabase;

    public MongoHealthIndicator(@Qualifier(MongoConfig.WRITE_DATABASE) MongoDatabase writeDatabase,
                                @Qualifier(MongoConfig.READ_DATABASE) MongoDatabase readDatabase) {
        this.writeDatabase = writeDatabase;
        this.readDatabase = readDatabase;
    }

    @Override
    public Health health() {
        boolean write = ping(writeDatabase);
        boolean read = ping(readDatabase);
        Health.Builder builder = write && read ? Health.up() : Health.down();
        return builder.withDetail("write", write ? "UP" : "DOWN").withDetail("read", read ? "UP" : "DOWN").build();
    }

    private static boolean ping(MongoDatabase database) {
        try {
            database.runCommand(PING);
            return true;
        } catch (MongoException e) {
            return false;
        }
    }
}
