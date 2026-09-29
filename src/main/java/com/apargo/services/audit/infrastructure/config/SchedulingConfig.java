package com.apargo.services.audit.infrastructure.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.apargo.services.audit.infrastructure.scheduling.MongoJobLock;
import com.mongodb.client.MongoDatabase;

/** Scheduling exists only on instances running the archiver role. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "audit.roles", name = "archiver", havingValue = "true")
public class SchedulingConfig {

    @Bean
    public MongoJobLock mongoJobLock(@Qualifier(MongoConfig.WRITE_DATABASE) MongoDatabase database, Clock clock) {
        return new MongoJobLock(database, clock);
    }
}
