package com.apargo.service.auditlog.consumer;

import com.apargo.service.auditlog.dto.request.AuditIngestRequest;
import com.apargo.service.auditlog.exception.DuplicateAuditEventException;
import com.apargo.service.auditlog.service.command.AuditCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * One consumer for every audit topic instead of one consumer class per
 * module (previously: DispatchAuditConsumer, ConversationAuditConsumer,
 * CampaignAuditConsumer, StorageFileAuditConsumer). Every topic carries the
 * same {@link AuditIngestRequest} JSON shape and lands in the same
 * AuditCommandService.ingest(...) call the REST controller uses — the only
 * thing that differs per topic is routing, which Spring Kafka handles via
 * the topics list below.
 * <p>
 * ack-mode is MANUAL (see application.yml): the offset is only committed
 * after a successful (or duplicate-rejected) write, so a crash mid-processing
 * results in redelivery rather than data loss. Duplicate redelivery is safe
 * because of the unique eventId index enforced in AuditCommandService.
 */
@Component
@ConditionalOnProperty(prefix = "audit.kafka", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class AuditKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditKafkaConsumer.class);

    private final AuditCommandService commandService;
    private final ObjectMapper objectMapper;

      @KafkaListener(
            topics = "${audit.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            AuditIngestRequest request = objectMapper.readValue(record.value(), AuditIngestRequest.class);
            commandService.ingest(request);
            ack.acknowledge();
        } catch (DuplicateAuditEventException e) {
            // Already logged inside AuditCommandService. Still ack — redelivering
            // a message we've already durably rejected forever would just loop.
            log.warn("audit_kafka_duplicate_acked topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset());
            ack.acknowledge();
        } catch (Exception e) {
            // Do NOT ack — let Kafka redeliver. If this becomes a poison-pill
            // message that fails every retry, route it to a dead-letter topic
            // via a DefaultErrorHandler/DeadLetterPublishingRecoverer instead
            // of leaving the consumer stuck on the same offset.
            log.error("audit_kafka_processing_failed topic={} partition={} offset={} error={}",
                    record.topic(), record.partition(), record.offset(), e.getMessage(), e);
        }
    }
}
