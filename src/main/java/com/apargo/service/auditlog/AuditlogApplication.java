package com.apargo.service.auditlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;


@SpringBootApplication(exclude = KafkaAutoConfiguration.class)
public class AuditlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuditlogApplication.class, args);
	}
}
