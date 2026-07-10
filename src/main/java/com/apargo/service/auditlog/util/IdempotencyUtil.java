package com.apargo.service.auditlog.util;

import com.apargo.service.auditlog.enums.Module;
import com.apargo.service.auditlog.enums.AuditEventType;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Guarantees every AuditLog row has a stable, unique eventId, whether the
 * producing service sent one explicitly or not.
 * <p>
 * REST callers are expected to send eventId themselves. Kafka messages that
 * omit it (or that come from an at-least-once topic where the caller can't
 * guarantee uniqueness) get a deterministic key derived from the event's
 * natural identity, so redelivery of the exact same message always resolves
 * to the exact same eventId and gets rejected as a duplicate by the unique
 * Mongo index rather than double-counted.
 */
public final class IdempotencyUtil {

    private IdempotencyUtil() {
    }

    public static String resolveEventId(String suppliedEventId, Module module, AuditEventType eventType,
                                         Long orgId, String entityType, String entityId, long occurredAtEpochMilli) {
        if (suppliedEventId != null && !suppliedEventId.isBlank()) {
            return suppliedEventId;
        }
        String natural = String.join("|",
                module.name(),
                eventType.name(),
                String.valueOf(orgId),
                Objects.toString(entityType, ""),
                Objects.toString(entityId, ""),
                String.valueOf(occurredAtEpochMilli));
        return "gen_" + sha256Hex(natural);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on the JVM; this branch is unreachable in practice.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
