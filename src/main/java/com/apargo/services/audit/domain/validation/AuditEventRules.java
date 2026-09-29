package com.apargo.services.audit.domain.validation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.apargo.platform.contract.audit.AuditEventDto;
import com.apargo.platform.contract.audit.AuditEventValidator;
import com.apargo.services.audit.common.enums.DeadLetterReason;

/**
 * Decides whether an audit event is stored.
 * <ul>
 *   <li><b>Blocking</b> (dead-lettered): anything storage and queries depend on.</li>
 *   <li><b>Reported</b> (stored anyway): every other finding of the contract's
 *       {@link AuditEventValidator}. A producer bug must never cost us the audit record.</li>
 * </ul>
 */
public final class AuditEventRules {

    /** Same limit as the {@code audit_logs} validator. */
    public static final int MAX_CHANGES = 100;

    private final AcceptancePolicy policy;

    public AuditEventRules(AcceptancePolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public ValidationResult evaluate(AuditEventDto event, Instant now) {
        if (event == null) {
            return ValidationResult.rejected(DeadLetterReason.VALIDATION, "event is null");
        }
        var envelope = EventFieldRules.checkEnvelope(policy, event.environment(), event.schemaVersion());
        if (envelope.isPresent()) {
            return envelope.get();
        }

        List<String> problems = EventFieldRules.checkIdentity(policy, event.eventId(), event.sourceService(),
                event.eventType(), event.occurredAt(), now);
        if (!EventFieldRules.matches(EventFieldRules.UPPER_SNAKE, event.module())) {
            problems.add("module is required, upper snake case");
        }
        if (event.status() == null) {
            problems.add("status is required");
        }
        if (event.orgId() == null || event.orgId() < 0) {
            problems.add("orgId is required (0 for platform-level data)");
        }
        if (event.actor() == null || event.actor().type() == null) {
            problems.add("actor.type is required");
        }
        if (event.entity() != null && EventFieldRules.isBlank(event.entity().type())) {
            problems.add("entity.type is required when entity is present");
        }
        if (event.changes().size() > MAX_CHANGES) {
            problems.add("changes has more than " + MAX_CHANGES + " entries");
        }
        if (!problems.isEmpty()) {
            return ValidationResult.rejected(DeadLetterReason.VALIDATION, problems);
        }
        return ValidationResult.accepted(AuditEventValidator.validate(event));
    }
}
