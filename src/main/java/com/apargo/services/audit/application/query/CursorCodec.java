package com.apargo.services.audit.application.query;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.apargo.platform.contract.event.EventIds;
import com.apargo.services.audit.common.error.ApiException;
import com.apargo.services.audit.common.error.FieldErrorCode;

/** Encodes the page position as an opaque, URL-safe cursor. Clients must not build or decode it. */
public final class CursorCodec {

    public static final String CURSOR_FIELD = "cursor";

    private static final String VERSION = "v1";
    private static final String SEPARATOR = ":";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private CursorCodec() {
    }

    public static String encode(PageCursor cursor) {
        String raw = VERSION + SEPARATOR + cursor.occurredAt().toEpochMilli() + SEPARATOR + cursor.eventId();
        return ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** @return {@code null} for a blank cursor (first page) */
    public static PageCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String[] parts = new String(DECODER.decode(cursor), StandardCharsets.UTF_8).split(SEPARATOR, 3);
            if (parts.length == 3 && VERSION.equals(parts[0]) && EventIds.isUuidV7(parts[2])) {
                return new PageCursor(Instant.ofEpochMilli(Long.parseLong(parts[1])), parts[2]);
            }
        } catch (IllegalArgumentException e) {
            // falls through to the validation error below (bad base64 or number)
        }
        throw ApiException.validation(CURSOR_FIELD, FieldErrorCode.INVALID_VALUE, "cursor is not valid");
    }
}
