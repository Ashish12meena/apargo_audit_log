package com.apargo.services.audit.application.port.out;

/**
 * A dependency (MongoDB, Kafka) is temporarily unable to do its job. Ingestion retries the whole
 * batch with back-off and never dead-letters because of it; the API answers 503.
 */
public class TransientFailureException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransientFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
