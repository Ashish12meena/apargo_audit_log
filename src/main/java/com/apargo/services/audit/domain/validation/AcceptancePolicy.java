package com.apargo.services.audit.domain.validation;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

import com.apargo.platform.contract.event.EventEnvironments;

/** What this service instance accepts: its environment, schema versions and clock-skew tolerance. */
public record AcceptancePolicy(String environment, Set<Integer> supportedSchemaVersions, Duration maxFutureSkew) {

    public AcceptancePolicy {
        if (!EventEnvironments.isValid(environment)) {
            throw new IllegalArgumentException("environment must be one of " + EventEnvironments.ALL);
        }
        Objects.requireNonNull(supportedSchemaVersions, "supportedSchemaVersions");
        if (supportedSchemaVersions.isEmpty()) {
            throw new IllegalArgumentException("supportedSchemaVersions must not be empty");
        }
        supportedSchemaVersions = Set.copyOf(supportedSchemaVersions);
        Objects.requireNonNull(maxFutureSkew, "maxFutureSkew");
    }

    public boolean supports(Integer schemaVersion) {
        return schemaVersion != null && supportedSchemaVersions.contains(schemaVersion);
    }
}
