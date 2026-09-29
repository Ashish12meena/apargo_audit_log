package com.apargo.services.audit.infrastructure.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.infrastructure.kafka.ConsumerCircuitBreaker;

/**
 * Reports listener and circuit state ({@code auditConsumer}). Stays UP while the circuit is open:
 * the consumer is healthy and retrying; restarting it would not help a store outage. Alert on
 * the {@code audit.consumer.circuit.open} metric and consumer lag instead.
 */
@Component("auditConsumerHealthIndicator")
@ConditionalOnProperty(prefix = "audit.roles", name = "consumer", havingValue = "true", matchIfMissing = true)
public class ConsumerHealthIndicator implements HealthIndicator {

    private final KafkaListenerEndpointRegistry registry;
    private final ConsumerCircuitBreaker circuitBreaker;

    public ConsumerHealthIndicator(KafkaListenerEndpointRegistry registry, ConsumerCircuitBreaker circuitBreaker) {
        this.registry = registry;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up()
                .withDetail("circuit", circuitBreaker.isOpen() ? "OPEN" : "CLOSED")
                .withDetail("consecutiveFailures", circuitBreaker.consecutiveFailures());
        for (MessageListenerContainer container : registry.getListenerContainers()) {
            builder.withDetail(container.getListenerId(), container.isRunning() ? "RUNNING" : "STOPPED");
        }
        return builder.build();
    }
}
