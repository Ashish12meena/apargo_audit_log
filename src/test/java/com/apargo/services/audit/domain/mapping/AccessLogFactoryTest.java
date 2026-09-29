package com.apargo.services.audit.domain.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.apargo.services.audit.TestEvents;
import com.apargo.services.audit.domain.model.AccessLog;
import com.apargo.services.audit.domain.policy.RetentionPolicy;

class AccessLogFactoryTest {

    private final AccessLogFactory factory =
            new AccessLogFactory(new RetentionPolicy(Duration.ofDays(30), Duration.ofDays(90)));

    @Test
    void usesTheAccessHotWindow() {
        var event = TestEvents.access("FAILURE", "INVALID_CREDENTIALS");

        AccessLog log = factory.create(event, TestEvents.NOW);

        assertThat(log.failureCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(log.actor().id()).isEqualTo("501");
        assertThat(log.archiveAt()).isEqualTo(TestEvents.NOW.plus(Duration.ofDays(90)));
    }
}
