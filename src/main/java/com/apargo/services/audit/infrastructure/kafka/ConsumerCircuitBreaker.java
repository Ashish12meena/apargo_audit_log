package com.apargo.services.audit.infrastructure.kafka;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.apargo.services.audit.common.constant.MetricNames;
import com.apargo.services.audit.infrastructure.config.AuditProperties;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Stops every listener thread from hammering a store that is down. After
 * {@code failure-threshold} consecutive failed batches the circuit opens: batches fail fast
 * (no store call) until {@code open-duration} has passed, then one trial batch runs. The error
 * handler keeps retrying with back-off throughout, so nothing is skipped or dead-lettered.
 */
@Component
public class ConsumerCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(ConsumerCircuitBreaker.class);

    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private volatile Instant openUntil;

    public ConsumerCircuitBreaker(AuditProperties properties, Clock clock, MeterRegistry registry) {
        this.failureThreshold = properties.consumer().circuitBreaker().failureThreshold();
        this.openDuration = properties.consumer().circuitBreaker().openDuration();
        this.clock = clock;
        Gauge.builder(MetricNames.CONSUMER_CIRCUIT_OPEN, this, breaker -> breaker.isOpen() ? 1 : 0)
                .description("1 while the ingestion circuit is open")
                .register(registry);
    }

    /** @throws TransientFailureException while the circuit is open */
    public void beforeBatch() {
        Instant until = openUntil;
        if (until != null && clock.instant().isBefore(until)) {
            throw new TransientFailureException("ingestion circuit open until " + until, null);
        }
    }

    public void onSuccess() {
        int failures = consecutiveFailures.getAndSet(0);
        openUntil = null;
        if (failures >= failureThreshold) {
            log.info("consumer_circuit_closed afterFailures={}", failures);
        }
    }

    public void onFailure(Throwable cause) {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= failureThreshold) {
            openUntil = clock.instant().plus(openDuration);
            if (failures == failureThreshold) {
                log.error("consumer_circuit_opened failures={} openFor={} cause={}", failures, openDuration,
                        cause.toString());
            }
        }
    }

    public boolean isOpen() {
        Instant until = openUntil;
        return until != null && clock.instant().isBefore(until);
    }

    public int consecutiveFailures() {
        return consecutiveFailures.get();
    }
}
