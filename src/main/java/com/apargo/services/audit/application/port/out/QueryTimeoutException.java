package com.apargo.services.audit.application.port.out;

/** A read query exceeded its time limit; the API answers 504. */
public class QueryTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QueryTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
