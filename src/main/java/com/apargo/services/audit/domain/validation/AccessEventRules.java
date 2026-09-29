package com.apargo.services.audit.domain.validation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.apargo.services.audit.common.enums.DeadLetterReason;
import com.apargo.services.audit.domain.event.AccessEventMessage;

/**
 * Decides whether an access event is stored. The contract has no access validator, so the
 * rules of the {@code AccessEventDto} Javadoc are applied here, split into blocking and reported.
 */
public final class AccessEventRules {

    private static final String FAILURE = "FAILURE";

    private final AcceptancePolicy policy;

    public AccessEventRules(AcceptancePolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public ValidationResult evaluate(AccessEventMessage event, Instant now) {
        if (event == null) {
            return ValidationResult.rejected(DeadLetterReason.VALIDATION, "event is null");
        }
        var envelope = EventFieldRules.checkEnvelope(policy, event.environment(), event.schemaVersion());
        if (envelope.isPresent()) {
            return envelope.get();
        }

        List<String> problems = EventFieldRules.checkIdentity(policy, event.eventId(), event.sourceService(),
                event.eventType(), event.occurredAt(), now);
        if (!EventFieldRules.matches(EventFieldRules.UPPER_SNAKE, event.status())) {
            problems.add("status is required, upper snake case");
        }
        if (event.orgId() != null && event.orgId() < 0) {
            problems.add("orgId must be 0 or more");
        }
        if (event.actor() == null || !EventFieldRules.matches(EventFieldRules.UPPER_SNAKE, event.actor().type())) {
            problems.add("actor.type is required, upper snake case");
        }
        if (!problems.isEmpty()) {
            return ValidationResult.rejected(DeadLetterReason.VALIDATION, problems);
        }

        List<String> reported = new ArrayList<>();
        if (!EventFieldRules.isValidTraceId(event.traceId())) {
            reported.add("traceId is required, 32 lowercase hex");
        }
        if (FAILURE.equals(event.status()) && EventFieldRules.isBlank(event.failureCode())) {
            reported.add("failureCode is required when status is FAILURE");
        }
        return ValidationResult.accepted(reported);
    }
}
