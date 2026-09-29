package com.apargo.services.audit.infrastructure.config;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.apargo.services.audit.api.context.InternalApiKeyFilter;
import com.apargo.services.audit.api.context.RequestContextFilter;
import com.apargo.services.audit.api.context.RequestContextResolver;
import com.apargo.services.audit.api.error.ErrorResponseWriter;
import com.apargo.services.audit.common.constant.ApiHeaders;

/**
 * HTTP plumbing: request filters (request id and trace id first, then the internal API key),
 * the {@code RequestContext} argument resolver, and CORS (all origins by default; empty list = off).
 */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private static final long CORS_MAX_AGE_SECONDS = 3600;

    private final AuditProperties properties;

    public WebConfig(AuditProperties properties) {
        this.properties = properties;
    }

    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>(new RequestContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<InternalApiKeyFilter> internalApiKeyFilter(ErrorResponseWriter errorWriter) {
        AuditProperties.Security security = properties.security();
        FilterRegistrationBean<InternalApiKeyFilter> registration = new FilterRegistrationBean<>(
                new InternalApiKeyFilter(security.internalApiKey(), security.allowedCallers(),
                        security.anyCallerAllowed(), errorWriter));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/internal/*");
        return registration;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new RequestContextResolver());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = properties.security().corsAllowedOrigins();
        if (origins.isEmpty()) {
            return;
        }
        // Origin patterns accept "*" (all origins, the default) as well as explicit origins.
        registry.addMapping("/**")
                .allowedOriginPatterns(origins.toArray(String[]::new))
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders(ApiHeaders.REQUEST_ID)
                .maxAge(CORS_MAX_AGE_SECONDS);
    }
}
