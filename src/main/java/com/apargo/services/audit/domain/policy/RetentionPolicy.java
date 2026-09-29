package com.apargo.services.audit.domain.policy;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import com.apargo.services.audit.common.enums.LogStream;

/**
 * How long each stream stays hot in MongoDB. {@code archiveAt} is computed from the server's
 * {@code recordedAt}, so producer clock skew never shortens retention.
 */
public record RetentionPolicy(Duration auditHot, Duration accessHot) {

    public RetentionPolicy {
        requirePositive(auditHot, "auditHot");
        requirePositive(accessHot, "accessHot");
    }

    public Instant archiveAt(LogStream stream, Instant recordedAt) {
        return recordedAt.plus(switch (stream) {
            case AUDIT -> auditHot;
            case ACCESS -> accessHot;
        });
    }

    private static void requirePositive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
