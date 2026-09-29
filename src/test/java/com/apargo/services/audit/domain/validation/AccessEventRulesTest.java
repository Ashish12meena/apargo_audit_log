package com.apargo.services.audit.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.apargo.services.audit.TestEvents;
import com.apargo.services.audit.common.enums.DeadLetterReason;
import com.apargo.services.audit.domain.event.AccessEventMessage;
import com.apargo.services.audit.domain.model.AccessActor;

class AccessEventRulesTest {

    private final AccessEventRules rules = new AccessEventRules(TestEvents.POLICY);

    @Test
    void acceptsAContractEvent() {
        ValidationResult result = rules.evaluate(TestEvents.access("FAILURE", "INVALID_CREDENTIALS"), TestEvents.NOW);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.reported()).isEmpty();
    }

    @Test
    void acceptsSchemaDocValuesOutsideTheContractEnums() {
        AccessEventMessage base = TestEvents.access("DENIED", null);
        AccessEventMessage event = new AccessEventMessage(base.eventId(), 1, "gateway", TestEvents.ENVIRONMENT,
                "PERMISSION_DENIED", "DENIED", 1001L, new AccessActor("API_KEY", "key-1", null, null, null), null,
                null, "POST /v1/campaigns", null, null, "IN", null, null, TestEvents.TRACE_ID, base.occurredAt());

        assertThat(rules.evaluate(event, TestEvents.NOW).isAccepted()).isTrue();
    }

    @Test
    void rejectsAMissingActorType() {
        AccessEventMessage base = TestEvents.access("SUCCESS", null);
        AccessEventMessage event = new AccessEventMessage(base.eventId(), 1, "auth-service", TestEvents.ENVIRONMENT,
                "LOGIN_SUCCEEDED", "SUCCESS", 1001L, new AccessActor(null, "501", null, null, null), null, null,
                null, null, null, null, null, null, TestEvents.TRACE_ID, base.occurredAt());

        ValidationResult result = rules.evaluate(event, TestEvents.NOW);

        assertThat(result.rejectReason()).isEqualTo(DeadLetterReason.VALIDATION);
    }

    @Test
    void reportsAFailureWithoutFailureCode() {
        ValidationResult result = rules.evaluate(TestEvents.access("FAILURE", null), TestEvents.NOW);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.reported()).containsExactly("failureCode is required when status is FAILURE");
    }
}
