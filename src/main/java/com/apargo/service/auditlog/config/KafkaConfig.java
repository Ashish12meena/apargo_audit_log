package com.apargo.service.auditlog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
@ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {
}
