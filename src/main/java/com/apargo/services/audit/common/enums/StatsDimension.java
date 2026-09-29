package com.apargo.services.audit.common.enums;

import java.util.Arrays;
import java.util.Optional;

/** Fields the stats endpoint may group by. Low-cardinality only, never ids or free text. */
public enum StatsDimension {

    MODULE("module"),
    EVENT_TYPE("eventType"),
    STATUS("status"),
    CHANNEL("channel"),
    SOURCE_SERVICE("sourceService"),
    ACTOR_TYPE("actorType"),
    PROJECT_ID("projectId");

    private final String apiName;

    StatsDimension(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }

    public static Optional<StatsDimension> fromApiName(String value) {
        return Arrays.stream(values()).filter(d -> d.apiName.equals(value)).findFirst();
    }
}
