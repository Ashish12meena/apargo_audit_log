package com.apargo.services.audit.infrastructure.kafka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import com.apargo.services.audit.application.ingest.InboundEvent;

/** Converts Kafka records to the transport-neutral {@link InboundEvent}. */
final class InboundEventAdapter {

    private static final byte[] EMPTY = new byte[0];

    private InboundEventAdapter() {
    }

    static List<InboundEvent> toEvents(List<ConsumerRecord<String, byte[]>> records) {
        List<InboundEvent> events = new ArrayList<>(records.size());
        for (ConsumerRecord<String, byte[]> record : records) {
            events.add(toEvent(record));
        }
        return events;
    }

    static InboundEvent toEvent(ConsumerRecord<String, byte[]> record) {
        Map<String, byte[]> headers = new HashMap<>();
        for (Header header : record.headers()) {
            headers.put(header.key(), header.value() == null ? EMPTY : header.value());
        }
        return new InboundEvent(record.topic(), record.partition(), record.offset(), record.key(), record.value(),
                headers);
    }
}
