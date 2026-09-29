package com.apargo.services.audit.infrastructure.kafka;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.ingest.IngestBatchService;
import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.apargo.services.audit.common.constant.LogKeys;
import com.apargo.services.audit.common.enums.LogStream;

/**
 * What both listeners do with a polled batch: circuit check, convert, ingest. Returning normally
 * lets the container commit the batch offsets; throwing hands the batch to the error handler,
 * which retries it with back-off.
 */
@Component
@ConditionalOnProperty(prefix = "audit.roles", name = "consumer", havingValue = "true", matchIfMissing = true)
public class StreamBatchHandler {

    private final IngestBatchService ingestService;
    private final ConsumerCircuitBreaker circuitBreaker;

    public StreamBatchHandler(IngestBatchService ingestService, ConsumerCircuitBreaker circuitBreaker) {
        this.ingestService = ingestService;
        this.circuitBreaker = circuitBreaker;
    }

    public void handle(LogStream stream, List<ConsumerRecord<String, byte[]>> records) {
        MDC.put(LogKeys.STREAM, stream.tag());
        try {
            circuitBreaker.beforeBatch();
            ingestService.ingest(stream, InboundEventAdapter.toEvents(records));
            circuitBreaker.onSuccess();
        } catch (TransientFailureException e) {
            circuitBreaker.onFailure(e);
            throw e;
        } finally {
            MDC.remove(LogKeys.STREAM);
        }
    }
}
