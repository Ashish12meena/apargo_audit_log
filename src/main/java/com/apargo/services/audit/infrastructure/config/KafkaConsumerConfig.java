package com.apargo.services.audit.infrastructure.config;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Error handling of the batch listeners. Spring Boot applies this handler to the auto-configured
 * container factory ({@code spring.kafka.listener.type=batch}, {@code ack-mode=batch}).
 * <p>
 * A listener exception means "the store or the DLQ could not take this batch right now" (poison
 * records never throw; they are dead-lettered inside the batch). The whole batch is retried in
 * memory with exponential back-off and <b>no attempt limit</b>, with the consumer paused and still
 * polling so the group does not rebalance. Offsets are committed only after a successful attempt,
 * so nothing is skipped.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "audit.roles", name = "consumer", havingValue = "true", matchIfMissing = true)
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public DefaultErrorHandler batchErrorHandler(AuditProperties properties) {
        AuditProperties.Retry retry = properties.consumer().retry();
        ExponentialBackOff backOff = new ExponentialBackOff(retry.initialInterval().toMillis(), retry.multiplier());
        backOff.setMaxInterval(retry.maxInterval().toMillis());
        backOff.setMaxElapsedTime(Long.MAX_VALUE);

        DefaultErrorHandler handler = new DefaultErrorHandler((record, exception) ->
                // Unreachable with an unlimited back-off; logged loudly in case configuration changes.
                log.error("ingest_retries_exhausted topic={} partition={} offset={}", record.topic(),
                        record.partition(), record.offset(), exception), backOff);
        // Every exception is retried. Spring Kafka's default "not retryable" list (e.g. ClassCastException)
        // would otherwise skip a batch on a bug; poison records never reach this handler.
        handler.setClassifications(Map.of(), true);
        handler.setRetryListeners(new BatchRetryLogger());
        return handler;
    }

    /** Logs each failed attempt once per batch, with the attempt number. */
    private static final class BatchRetryLogger implements RetryListener {

        @Override
        public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
            log.warn("ingest_record_retry topic={} partition={} offset={} attempt={} cause={}", record.topic(),
                    record.partition(), record.offset(), deliveryAttempt, rootMessage(ex));
        }

        @Override
        public void failedDelivery(ConsumerRecords<?, ?> records, Exception ex, int deliveryAttempt) {
            log.warn("ingest_batch_retry records={} partitions={} attempt={} cause={}", records.count(),
                    records.partitions(), deliveryAttempt, rootMessage(ex));
        }

        private static String rootMessage(Throwable ex) {
            Throwable root = ex;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            return root.getClass().getSimpleName() + ": " + root.getMessage();
        }
    }
}
