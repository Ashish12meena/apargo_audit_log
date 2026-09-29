package com.apargo.services.audit.common.enums;

import java.util.Arrays;
import java.util.Optional;

/** Time bucket for stats, with the UTC date format of its label. */
public enum TimeBucket {

    HOUR("hour", "%Y-%m-%dT%H:00:00Z"),
    DAY("day", "%Y-%m-%d");

    private final String apiName;
    private final String dateFormat;

    TimeBucket(String apiName, String dateFormat) {
        this.apiName = apiName;
        this.dateFormat = dateFormat;
    }

    public String apiName() {
        return apiName;
    }

    /** MongoDB $dateToString format. */
    public String dateFormat() {
        return dateFormat;
    }

    public static Optional<TimeBucket> fromApiName(String value) {
        return Arrays.stream(values()).filter(b -> b.apiName.equals(value)).findFirst();
    }
}
