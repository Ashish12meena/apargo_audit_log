package com.apargo.services.audit.common.enums;

import com.apargo.services.audit.common.constant.MongoCollections;

/** One event stream: its own topic, consumer group, collection and metric tag. */
public enum LogStream {

    AUDIT(MongoCollections.AUDIT_LOGS, "audit"),
    ACCESS(MongoCollections.ACCESS_LOGS, "access");

    private final String collection;
    private final String tag;

    LogStream(String collection, String tag) {
        this.collection = collection;
        this.tag = tag;
    }

    public String collection() {
        return collection;
    }

    /** Lower-case name for metrics and log fields. */
    public String tag() {
        return tag;
    }
}
