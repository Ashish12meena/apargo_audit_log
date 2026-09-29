package com.apargo.services.audit.domain.validation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.apargo.platform.contract.event.EventIds;
import com.apargo.services.audit.common.enums.DeadLetterReason;

/** Rules shared by audit and access events. Patterns are the contract's own. */
final class EventFieldRules {

    /** {@code <ENTITY>_<PAST_VERB>}: upper snake case, at least two words. */
    static final Pattern EVENT_TYPE = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)+");
    /** Upper snake case, one or more words. */
    static final Pattern UPPER_SNAKE = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");
    /** W3C trace id: 32 lowercase hex. */
    static final Pattern TRACE_ID = Pattern.compile("[0-9a-f]{32}");

    static final int MAX_SOURCE_SERVICE_LENGTH = 100;

    private EventFieldRules() {
    }

    /**
     * Environment and schema version are checked first because they get their own dead-letter
     * reasons; replaying them is a deployment decision, not a producer fix.
     */
    static Optional<ValidationResult> checkEnvelope(AcceptancePolicy policy, String environment, Integer schemaVersion) {
        if (!policy.environment().equals(environment)) {
            return Optional.of(ValidationResult.rejected(DeadLetterReason.ENV_MISMATCH,
                    "environment '" + environment + "' does not match '" + policy.environment() + "'"));
        }
        if (!policy.supports(schemaVersion)) {
            return Optional.of(ValidationResult.rejected(DeadLetterReason.UNSUPPORTED_VERSION,
                    "schemaVersion " + schemaVersion + " is not supported " + policy.supportedSchemaVersions()));
        }
        return Optional.empty();
    }

    /** Blocking checks every event needs to be stored and queried. */
    static List<String> checkIdentity(AcceptancePolicy policy, String eventId, String sourceService,
                                      String eventType, Instant occurredAt, Instant now) {
        List<String> problems = new ArrayList<>();
        if (!EventIds.isUuidV7(eventId)) {
            problems.add("eventId must be a UUIDv7");
        }
        if (isBlank(sourceService)) {
            problems.add("sourceService is required");
        } else if (sourceService.length() > MAX_SOURCE_SERVICE_LENGTH) {
            problems.add("sourceService is longer than " + MAX_SOURCE_SERVICE_LENGTH);
        }
        if (!matches(EVENT_TYPE, eventType)) {
            problems.add("eventType is required, <ENTITY>_<PAST_VERB>");
        }
        if (occurredAt == null) {
            problems.add("occurredAt is required");
        } else if (occurredAt.isAfter(now.plus(policy.maxFutureSkew()))) {
            problems.add("occurredAt is more than " + policy.maxFutureSkew() + " in the future");
        }
        return problems;
    }

    static boolean matches(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
    }

    static boolean isValidTraceId(String traceId) {
        return matches(TRACE_ID, traceId) && !traceId.chars().allMatch(c -> c == '0');
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
