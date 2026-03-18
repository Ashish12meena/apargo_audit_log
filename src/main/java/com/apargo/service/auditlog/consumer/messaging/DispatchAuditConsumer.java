package com.apargo.service.auditlog.consumer.messaging;

import com.apargo.service.auditlog.dto.request.ingest.messaging.dispatch.*;
import com.apargo.service.auditlog.service.ingestion.messaging.DispatchAuditIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchAuditConsumer {

    private final DispatchAuditIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${messaging.audit.topics.dispatch}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {

        String eventType = extractEventType(record);
        if (eventType == null) {
            log.error("Missing auditEventType header — skipping unroutable message");
            ack.acknowledge();
            return;
        }

        try {
            route(eventType, record.value());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process dispatch audit event: eventType={}", eventType, e);
        }
    }

    private void route(String eventType, String json) throws Exception {
        switch (eventType) {
            case "KAFKA_BATCH_PUBLISHED" -> ingestionService.handleBatchPublished(
                    objectMapper.readValue(json, KafkaBatchPublishedIngestRequest.class));

            case "KAFKA_PUBLISH_FAILED" -> ingestionService.handlePublishFailed(
                    objectMapper.readValue(json, KafkaPublishFailedIngestRequest.class));

            case "BATCH_PARTIALLY_FAILED" -> ingestionService.handlePartiallyFailed(
                    objectMapper.readValue(json, BatchPartiallyFailedIngestRequest.class));

            case "STALE_LOCK_RECOVERED" -> ingestionService.handleStaleLockRecovered(
                    objectMapper.readValue(json, StaleLockRecoveredIngestRequest.class));

            case "RECIPIENT_MAX_ATTEMPTS_EXHAUSTED" -> ingestionService.handleMaxAttempts(
                    objectMapper.readValue(json, RecipientMaxAttemptsIngestRequest.class));

            default -> log.warn("Unknown dispatch audit eventType='{}' — skipping", eventType);
        }
    }

    private String extractEventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("auditEventType");
        if (header == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}