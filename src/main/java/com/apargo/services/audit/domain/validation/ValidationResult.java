package com.apargo.services.audit.domain.validation;

import java.util.List;

import com.apargo.services.audit.common.enums.DeadLetterReason;

/**
 * Outcome of checking one event. A rejected event goes to the dead-letter topic with
 * {@code rejectReason}; an accepted event is stored, and {@code reported} lists contract problems
 * that are logged and counted but do not block storage.
 */
public record ValidationResult(DeadLetterReason rejectReason, List<String> problems, List<String> reported) {

    public ValidationResult {
        problems = List.copyOf(problems);
        reported = List.copyOf(reported);
    }

    public static ValidationResult accepted(List<String> reported) {
        return new ValidationResult(null, List.of(), reported);
    }

    public static ValidationResult rejected(DeadLetterReason reason, List<String> problems) {
        return new ValidationResult(reason, problems, List.of());
    }

    public static ValidationResult rejected(DeadLetterReason reason, String problem) {
        return rejected(reason, List.of(problem));
    }

    public boolean isAccepted() {
        return rejectReason == null;
    }
}
