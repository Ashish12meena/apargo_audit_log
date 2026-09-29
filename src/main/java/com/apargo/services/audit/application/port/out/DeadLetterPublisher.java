package com.apargo.services.audit.application.port.out;

import java.util.List;

import com.apargo.services.audit.application.ingest.DeadLetter;
import com.apargo.services.audit.common.enums.LogStream;

/** Publishes poison records to the stream's dead-letter topic. */
public interface DeadLetterPublisher {

    /**
     * Returns only after every record is acknowledged by the broker.
     *
     * @throws TransientFailureException when any record could not be published
     */
    void publish(LogStream stream, List<DeadLetter> deadLetters);
}
