package com.apargo.services.audit.application.query;

import java.time.Instant;

/** Position after the last item of a page: its occurredAt and eventId (the sort keys). */
public record PageCursor(Instant occurredAt, String eventId) {
}
