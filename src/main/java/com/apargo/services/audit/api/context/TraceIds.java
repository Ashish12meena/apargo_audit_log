package com.apargo.services.audit.api.context;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/** W3C trace ids: 32 lowercase hex, never all zeros. */
final class TraceIds {

    private static final Pattern TRACEPARENT = Pattern.compile("00-([0-9a-f]{32})-[0-9a-f]{16}-[0-9a-f]{2}");
    private static final String ALL_ZERO = "0".repeat(32);
    private static final SecureRandom RANDOM = new SecureRandom();

    private TraceIds() {
    }

    static String newTraceId() {
        byte[] bytes = new byte[16];
        String id;
        do {
            RANDOM.nextBytes(bytes);
            id = HexFormat.of().formatHex(bytes);
        } while (ALL_ZERO.equals(id));
        return id;
    }

    static Optional<String> fromTraceparent(String header) {
        if (header == null) {
            return Optional.empty();
        }
        var matcher = TRACEPARENT.matcher(header.trim());
        if (!matcher.matches() || ALL_ZERO.equals(matcher.group(1))) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }
}
