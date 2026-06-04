package com.apargo.service.auditlog.consumer.messaging;

import com.apargo.service.auditlog.dto.request.ingest.messaging.campaign.*;
import com.apargo.service.auditlog.service.ingestion.messaging.CampaignAuditIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class CampaignAuditConsumer {

    private final CampaignAuditIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${messaging.audit.topics.campaign}",
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
            log.error("Failed to process campaign audit event: eventType={}", eventType, e);
            // do not ack — Spring Kafka retries then routes to DLT
        }
    }

    private void route(String eventType, String json) throws Exception {
        switch (eventType) {
            case "CAMPAIGN_CREATED" -> ingestionService.handleCreated(
                    objectMapper.readValue(json, CampaignCreatedIngestRequest.class));

            case "CAMPAIGN_SCHEDULED" -> ingestionService.handleScheduled(
                    objectMapper.readValue(json, CampaignScheduledIngestRequest.class));

            case "CAMPAIGN_RESCHEDULED" -> ingestionService.handleRescheduled(
                    objectMapper.readValue(json, CampaignRescheduledIngestRequest.class));

            case "PREPARATION_STARTED" -> ingestionService.handlePreparationStarted(
                    objectMapper.readValue(json, PreparationStartedIngestRequest.class));

            case "PREPARATION_FAILED" -> ingestionService.handlePreparationFailed(
                    objectMapper.readValue(json, PreparationFailedIngestRequest.class));

            case "DISPATCH_STARTED" -> ingestionService.handleDispatchStarted(
                    objectMapper.readValue(json, DispatchStartedIngestRequest.class));

            case "CAMPAIGN_PAUSED" -> ingestionService.handlePaused(
                    objectMapper.readValue(json, CampaignPausedIngestRequest.class));

            case "CAMPAIGN_RESUMED" -> ingestionService.handleResumed(
                    objectMapper.readValue(json, CampaignResumedIngestRequest.class));

            case "CAMPAIGN_CANCELLED" -> ingestionService.handleCancelled(
                    objectMapper.readValue(json, CampaignCancelledIngestRequest.class));

            case "CAMPAIGN_COMPLETED" -> ingestionService.handleCompleted(
                    objectMapper.readValue(json, CampaignCompletedIngestRequest.class));

            case "CAMPAIGN_FAILED" -> ingestionService.handleFailed(
                    objectMapper.readValue(json, CampaignFailedIngestRequest.class));

            default -> log.warn("Unknown campaign audit eventType='{}' — skipping", eventType);
        }
    }

    private String extractEventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("auditEventType");
        if (header == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}