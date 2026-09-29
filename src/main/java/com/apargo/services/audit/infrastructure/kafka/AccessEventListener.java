package com.apargo.services.audit.infrastructure.kafka;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.infrastructure.config.AuditDefaults;
import com.apargo.services.audit.common.enums.LogStream;

/** Consumes the access topic in batches ({@code AccessEventDto} JSON and its optional extras). */
@Component
@ConditionalOnProperty(prefix = "audit.roles", name = "consumer", havingValue = "true", matchIfMissing = true)
public class AccessEventListener {

    public static final String LISTENER_ID = "access-events";

    private final StreamBatchHandler handler;

    public AccessEventListener(StreamBatchHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(id = LISTENER_ID,
            topics = AuditDefaults.Placeholders.ACCESS_TOPIC,
            groupId = AuditDefaults.Placeholders.ACCESS_GROUP_ID,
            concurrency = AuditDefaults.Placeholders.ACCESS_CONCURRENCY,
            batch = "true")
    public void onBatch(List<ConsumerRecord<String, byte[]>> records) {
        handler.handle(LogStream.ACCESS, records);
    }
}
