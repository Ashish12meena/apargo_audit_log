package com.apargo.services.audit.infrastructure.kafka;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.infrastructure.config.AuditDefaults;
import com.apargo.services.audit.common.enums.LogStream;

/** Consumes the audit topic in batches ({@code AuditEventDto} JSON). */
@Component
@ConditionalOnProperty(prefix = "audit.roles", name = "consumer", havingValue = "true", matchIfMissing = true)
public class AuditEventListener {

    public static final String LISTENER_ID = "audit-events";

    private final StreamBatchHandler handler;

    public AuditEventListener(StreamBatchHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(id = LISTENER_ID,
            topics = AuditDefaults.Placeholders.AUDIT_TOPIC,
            groupId = AuditDefaults.Placeholders.AUDIT_GROUP_ID,
            concurrency = AuditDefaults.Placeholders.AUDIT_CONCURRENCY,
            batch = "true")
    public void onBatch(List<ConsumerRecord<String, byte[]>> records) {
        handler.handle(LogStream.AUDIT, records);
    }
}
