package com.apargo.services.audit.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.apargo.platform.contract.audit.AuditActorDto;
import com.apargo.platform.contract.audit.AuditChangeDto;
import com.apargo.platform.contract.audit.AuditEventStatus;
import com.apargo.services.audit.TestEvents;
import com.apargo.services.audit.common.enums.DeadLetterReason;

class AuditEventRulesTest {

    private final AuditEventRules rules = new AuditEventRules(TestEvents.POLICY);

    @Test
    void acceptsAValidEventWithNothingReported() {
        ValidationResult result = rules.evaluate(TestEvents.audit(), TestEvents.NOW);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.reported()).isEmpty();
    }

    @Test
    void rejectsAnotherEnvironmentWithItsOwnReason() {
        ValidationResult result = rules.evaluate(TestEvents.auditBuilder().environment("production").build(),
                TestEvents.NOW);

        assertThat(result.rejectReason()).isEqualTo(DeadLetterReason.ENV_MISMATCH);
    }

    @Test
    void rejectsAnUnsupportedSchemaVersion() {
        ValidationResult result = rules.evaluate(TestEvents.auditBuilder().schemaVersion(2).build(), TestEvents.NOW);

        assertThat(result.rejectReason()).isEqualTo(DeadLetterReason.UNSUPPORTED_VERSION);
    }

    @Test
    void rejectsBlockingProblemsAllAtOnce() {
        var event = TestEvents.auditBuilder()
                .eventId("not-a-uuid")
                .orgId(null)
                .actor(new AuditActorDto(null, "501", null, null))
                .eventType("updated")
                .build();

        ValidationResult result = rules.evaluate(event, TestEvents.NOW);

        assertThat(result.rejectReason()).isEqualTo(DeadLetterReason.VALIDATION);
        assertThat(result.problems()).hasSize(4);
    }

    @Test
    void rejectsAnEventTooFarInTheFuture() {
        var event = TestEvents.auditBuilder().occurredAt(TestEvents.NOW.plus(Duration.ofDays(2))).build();

        assertThat(rules.evaluate(event, TestEvents.NOW).rejectReason()).isEqualTo(DeadLetterReason.VALIDATION);
    }

    @Test
    void rejectsMoreChangesThanTheCollectionAllows() {
        List<AuditChangeDto> changes = new ArrayList<>();
        for (int i = 0; i <= AuditEventRules.MAX_CHANGES; i++) {
            changes.add(new AuditChangeDto("field" + i, null, i));
        }

        var result = rules.evaluate(TestEvents.auditBuilder().changes(changes).build(), TestEvents.NOW);

        assertThat(result.rejectReason()).isEqualTo(DeadLetterReason.VALIDATION);
    }

    @Test
    void storesButReportsNonBlockingContractProblems() {
        var event = TestEvents.auditBuilder()
                .traceId(null)
                .status(AuditEventStatus.FAILURE)
                .build();

        ValidationResult result = rules.evaluate(event, TestEvents.NOW);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.reported())
                .contains("traceId is required, 32 lowercase hex", "error is required when status is FAILURE");
    }
}
