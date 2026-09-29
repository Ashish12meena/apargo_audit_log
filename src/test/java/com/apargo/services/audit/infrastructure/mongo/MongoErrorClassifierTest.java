package com.apargo.services.audit.infrastructure.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.apargo.services.audit.application.port.out.QueryTimeoutException;
import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.mongodb.MongoException;
import com.mongodb.MongoExecutionTimeoutException;

class MongoErrorClassifierTest {

    @Test
    void knowsTransientServerCodes() {
        assertThat(MongoErrorClassifier.isTransientCode(10107)).isTrue(); // NotWritablePrimary
        assertThat(MongoErrorClassifier.isTransientCode(121)).isFalse();  // DocumentValidationFailure
        assertThat(MongoErrorClassifier.isTransientCode(MongoErrorClassifier.DUPLICATE_KEY)).isFalse();
    }

    @Test
    void mapsReadTimeoutsToQueryTimeout() {
        assertThatThrownBy(() -> MongoErrorClassifier.read("search", () -> {
            throw new MongoExecutionTimeoutException(50, "operation exceeded time limit");
        })).isInstanceOf(QueryTimeoutException.class);
    }

    @Test
    void mapsOtherReadFailuresToTransient() {
        assertThatThrownBy(() -> MongoErrorClassifier.read("search", () -> {
            throw new MongoException(6, "host unreachable");
        })).isInstanceOf(TransientFailureException.class);
    }
}
