package com.apargo.services.audit.infrastructure.kafka;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.infrastructure.config.AuditDefaults;
import com.apargo.services.audit.application.ingest.DeadLetter;
import com.apargo.services.audit.application.ingest.InboundEvent;
import com.apargo.services.audit.application.port.out.DeadLetterPublisher;
import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.apargo.services.audit.common.constant.DeadLetterHeaders;
import com.apargo.services.audit.common.enums.LogStream;
import com.apargo.services.audit.infrastructure.config.AuditProperties;

/**
 * Publishes dead letters to {@code <topic><dlq-suffix>} with the original key, value and headers
 * unchanged plus {@code x-dlq-*} headers, and waits for every broker acknowledgement.
 */
@Component
@ConditionalOnProperty(prefix = "audit.roles", name = "consumer", havingValue = "true", matchIfMissing = true)
public class KafkaDeadLetterPublisher implements DeadLetterPublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final AuditProperties.Topics topics;
    private final Duration sendTimeout;
    private final String applicationName;
    private final Clock clock;

    public KafkaDeadLetterPublisher(KafkaTemplate<String, byte[]> kafkaTemplate, AuditProperties properties,
                                    @Value(AuditDefaults.Placeholders.APPLICATION_NAME) String applicationName, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = properties.topics();
        this.sendTimeout = properties.consumer().deadLetterSendTimeout();
        this.applicationName = applicationName;
        this.clock = clock;
    }

    @Override
    public void publish(LogStream stream, List<DeadLetter> deadLetters) {
        if (deadLetters.isEmpty()) {
            return;
        }
        String topic = topics.deadLetterTopic(stream);
        try {
            List<CompletableFuture<?>> sends = new ArrayList<>(deadLetters.size());
            for (DeadLetter deadLetter : deadLetters) {
                sends.add(kafkaTemplate.send(toRecord(topic, stream, deadLetter)));
            }
            CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new))
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransientFailureException("interrupted while publishing to " + topic, e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            throw new TransientFailureException("publishing " + deadLetters.size() + " dead letters to " + topic
                    + " failed", e);
        }
    }

    private ProducerRecord<String, byte[]> toRecord(String topic, LogStream stream, DeadLetter deadLetter) {
        InboundEvent source = deadLetter.source();
        RecordHeaders headers = new RecordHeaders();
        for (Map.Entry<String, byte[]> header : source.headers().entrySet()) {
            headers.add(header.getKey(), header.getValue());
        }
        add(headers, DeadLetterHeaders.REASON, deadLetter.reason().name());
        add(headers, DeadLetterHeaders.DETAIL, cap(deadLetter.detail()));
        add(headers, DeadLetterHeaders.STREAM, stream.tag());
        add(headers, DeadLetterHeaders.ORIGINAL_TOPIC, source.topic());
        add(headers, DeadLetterHeaders.ORIGINAL_PARTITION, String.valueOf(source.partition()));
        add(headers, DeadLetterHeaders.ORIGINAL_OFFSET, String.valueOf(source.offset()));
        add(headers, DeadLetterHeaders.FAILED_AT, clock.instant().toString());
        add(headers, DeadLetterHeaders.SOURCE_APP, applicationName);
        return new ProducerRecord<>(topic, null, source.key(), source.value(), headers);
    }

    private static void add(RecordHeaders headers, String key, String value) {
        headers.add(key, (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String cap(String detail) {
        if (detail == null || detail.length() <= DeadLetterHeaders.MAX_DETAIL_LENGTH) {
            return detail;
        }
        return detail.substring(0, DeadLetterHeaders.MAX_DETAIL_LENGTH) + "...";
    }
}
