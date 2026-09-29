package com.apargo.services.audit.infrastructure.mongo;

import java.util.Set;
import java.util.function.Supplier;

import com.apargo.services.audit.application.port.out.QueryTimeoutException;
import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.mongodb.MongoException;
import com.mongodb.MongoExecutionTimeoutException;

/**
 * Sorts MongoDB failures into the three outcomes the service acts on:
 * duplicate (already stored), transient (retry the batch), or a rejection of one document.
 * Failures of a whole operation are always treated as transient: retrying and alerting is safer
 * than dead-lettering a full batch because of, say, a credentials problem.
 */
final class MongoErrorClassifier {

    static final int DUPLICATE_KEY = 11000;
    static final int MAX_TIME_MS_EXPIRED = 50;

    /** Server error codes that mean "the server could not do it right now". */
    private static final Set<Integer> TRANSIENT_CODES = Set.of(
            6,      // HostUnreachable
            7,      // HostNotFound
            50,     // MaxTimeMSExpired
            64,     // WriteConcernFailed
            89,     // NetworkTimeout
            91,     // ShutdownInProgress
            112,    // WriteConflict
            133,    // FailedToSatisfyReadPreference
            189,    // PrimarySteppedDown
            262,    // ExceededTimeLimit
            9001,   // SocketException
            10107,  // NotWritablePrimary
            11600,  // InterruptedAtShutdown
            11602,  // InterruptedDueToReplStateChange
            13435,  // NotPrimaryNoSecondaryOk
            13436); // NotPrimaryOrSecondary

    private static final int MAX_MESSAGE_LENGTH = 300;

    private MongoErrorClassifier() {
    }

    static boolean isTransientCode(int code) {
        return TRANSIENT_CODES.contains(code);
    }

    /** Runs a read and maps driver failures to the application's read exceptions. */
    static <T> T read(String operation, Supplier<T> query) {
        try {
            return query.get();
        } catch (MongoExecutionTimeoutException e) {
            throw new QueryTimeoutException(operation + " exceeded its time limit", e);
        } catch (MongoException e) {
            if (e.getCode() == MAX_TIME_MS_EXPIRED) {
                throw new QueryTimeoutException(operation + " exceeded its time limit", e);
            }
            throw new TransientFailureException(operation + " failed: " + describe(e), e);
        }
    }

    /** Runs a write-side operation (not the bulk insert) and maps every driver failure to transient. */
    static <T> T write(String operation, Supplier<T> action) {
        try {
            return action.get();
        } catch (MongoException e) {
            throw new TransientFailureException(operation + " failed: " + describe(e), e);
        }
    }

    static String describe(MongoException e) {
        return e.getClass().getSimpleName() + " code=" + e.getCode() + " " + truncate(e.getMessage());
    }

    static String truncate(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }
}
