package com.apargo.services.audit.infrastructure.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.apargo.services.audit.api.context.RequestContext;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** OpenAPI metadata. Disabled in prod unless API_DOCS_ENABLED=true. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    static {
        // Resolved from headers, not a query parameter.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(RequestContext.class);
    }

    @Bean
    public OpenAPI auditServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Audit Service API")
                .version("v1")
                .description("Read APIs for audit and access logs. Tenant scope comes from X-Org-Id / X-Project-Id; "
                        + "lists use cursor paging (size, cursor). Responses use the platform wrapper. "
                        + "/internal/** requires X-Internal-Api-Key and X-Internal-Caller."));
    }
}
