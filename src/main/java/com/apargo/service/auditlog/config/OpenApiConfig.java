package com.apargo.service.auditlog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes Swagger UI at /swagger-ui.html and the raw OpenAPI JSON at
 * /v3/api-docs. This matters more for an internal audit service than most
 * — every producing microservice (template, messaging, storage, wallet...)
 * needs to know the exact AuditIngestRequest shape and enum values, and a
 * live, always-in-sync contract beats a doc that goes stale in a wiki.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI auditLogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Audit Log Service API")
                        .description("Centralized audit trail for all Apargo/Aigreentick modules "
                                + "(template, messaging, storage, wallet, users, projects, plans, waba). "
                                + "Ingest via REST or Kafka; query via GET /api/v1/audit-logs.")
                        .version("v1")
                        .contact(new Contact().name("Apargo Platform Team")));
    }
}
