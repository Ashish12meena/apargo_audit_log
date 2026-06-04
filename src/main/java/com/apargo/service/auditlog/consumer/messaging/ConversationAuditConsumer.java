package com.apargo.service.auditlog.consumer.messaging;

import com.apargo.service.auditlog.dto.request.ingest.messaging.conversation.*;
import com.apargo.service.auditlog.service.ingestion.messaging.ConversationAuditIngestionService;
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
public class ConversationAuditConsumer {

    private final ConversationAuditIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${messaging.audit.topics.conversation}",
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
            log.error("Failed to process conversation audit event: eventType={}", eventType, e);
        }
    }

    private void route(String eventType, String json) throws Exception {
        switch (eventType) {
            case "CONVERSATION_CREATED" -> ingestionService.handleCreated(
                    objectMapper.readValue(json, ConversationCreatedIngestRequest.class));

            case "CONVERSATION_ASSIGNED" -> ingestionService.handleAssigned(
                    objectMapper.readValue(json, ConversationAssignedIngestRequest.class));

            case "CONVERSATION_REASSIGNED" -> ingestionService.handleReassigned(
                    objectMapper.readValue(json, ConversationReassignedIngestRequest.class));

            case "CONVERSATION_UNASSIGNED" -> ingestionService.handleUnassigned(
                    objectMapper.readValue(json, ConversationUnassignedIngestRequest.class));

            case "CONVERSATION_CLOSED" -> ingestionService.handleClosed(
                    objectMapper.readValue(json, ConversationClosedIngestRequest.class));

            case "CONVERSATION_REOPENED" -> ingestionService.handleReopened(
                    objectMapper.readValue(json, ConversationReopenedIngestRequest.class));

            case "CONVERSATION_ARCHIVED" -> ingestionService.handleArchived(
                    objectMapper.readValue(json, ConversationArchivedIngestRequest.class));

            default -> log.warn("Unknown conversation audit eventType='{}' — skipping", eventType);
        }
    }

    private String extractEventType(ConsumerRecord<String, String> record) {
        var header = record.headers().lastHeader("auditEventType");
        if (header == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}