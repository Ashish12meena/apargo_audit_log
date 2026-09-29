package com.apargo.services.audit.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/** Defaults are defined twice (application.yml and AuditDefaults); this keeps them identical. */
class AuditPropertiesDefaultsTest {

    @Test
    void yamlDefaultsEqualJavaDefaults() throws IOException {
        AuditProperties fromYaml = Binder.get(yamlOnlyEnvironment()).bind("audit", AuditProperties.class).get();

        assertThat(fromYaml).isEqualTo(AuditProperties.defaults());
    }

    @Test
    void missingKeysFallBackToDefaults() {
        StandardEnvironment environment = emptyEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("partial", Map.of(
                "audit.consumer.audit.concurrency", "6",
                "audit.query.max-range", "14d")));

        AuditProperties properties = Binder.get(environment).bindOrCreate("audit", AuditProperties.class);

        assertThat(properties.consumer().audit().concurrency()).isEqualTo(6);
        assertThat(properties.consumer().audit().groupId()).isEqualTo(AuditDefaults.AUDIT_GROUP_ID);
        assertThat(properties.consumer().access().concurrency()).isEqualTo(AuditDefaults.ACCESS_CONCURRENCY);
        assertThat(properties.query().maxRange()).isEqualTo(Duration.ofDays(14));
        assertThat(properties.query().defaultRange()).isEqualTo(AuditDefaults.QUERY_DEFAULT_RANGE);
        assertThat(properties.topics().audit()).isEqualTo(AuditDefaults.AUDIT_TOPIC);
    }

    @Test
    void blankTextFallsBackButInvalidValuesStillFail() {
        StandardEnvironment environment = emptyEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("values", Map.of(
                "audit.mongo.database", " ",
                "audit.environment", "qa")));

        assertThatThrownBy(() -> Binder.get(environment).bind("audit", AuditProperties.class))
                .isInstanceOf(BindException.class)
                .rootCause().hasMessageContaining("audit.environment");
    }

    /** application.yml with its own ${ENV:default} values only: no OS environment, no system properties. */
    private static StandardEnvironment yamlOnlyEnvironment() throws IOException {
        StandardEnvironment environment = emptyEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))
                .forEach(environment.getPropertySources()::addLast);
        return environment;
    }

    private static StandardEnvironment emptyEnvironment() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        return environment;
    }
}
