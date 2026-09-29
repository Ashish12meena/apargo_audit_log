package com.apargo.services.audit.domain.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.apargo.services.audit.TestEvents;
import com.apargo.services.audit.domain.model.AuditLog;
import com.apargo.services.audit.domain.policy.RetentionPolicy;

class AuditLogFactoryTest {

    private final AuditLogFactory factory =
            new AuditLogFactory(new RetentionPolicy(Duration.ofDays(30), Duration.ofDays(90)));

    @Test
    void copiesTheContractFieldsAndSetsServerTimes() {
        var event = TestEvents.audit();

        AuditLog log = factory.create(event, TestEvents.NOW);

        assertThat(log.eventId()).isEqualTo(event.eventId());
        assertThat(log.orgId()).isEqualTo(1001L);
        assertThat(log.actor()).isEqualTo(event.actor());
        assertThat(log.changes()).isEqualTo(event.changes());
        assertThat(log.metadata()).containsEntry("wabaId", "waba-001");
        assertThat(log.occurredAt()).isEqualTo(event.occurredAt());
        assertThat(log.recordedAt()).isEqualTo(TestEvents.NOW);
        assertThat(log.archiveAt()).isEqualTo(TestEvents.NOW.plus(Duration.ofDays(30)));
    }
}
