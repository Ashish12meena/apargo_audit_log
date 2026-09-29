package com.apargo.services.audit.application.ingest;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * One record as received from the broker, before any parsing. The raw value and headers are
 * kept so a poison record can be dead-lettered byte-for-byte.
 */
public record InboundEvent(String topic, int partition, long offset, String key, byte[] value,
                           Map<String, byte[]> headers) {

    public InboundEvent {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    /** Header value as UTF-8 text, or {@code null} when absent. */
    public String headerText(String name) {
        byte[] raw = headers.get(name);
        return raw == null ? null : new String(raw, StandardCharsets.UTF_8);
    }

    public int size() {
        return value == null ? 0 : value.length;
    }

    /** {@code topic-partition@offset}, for logs. */
    public String location() {
        return topic + "-" + partition + "@" + offset;
    }
}
