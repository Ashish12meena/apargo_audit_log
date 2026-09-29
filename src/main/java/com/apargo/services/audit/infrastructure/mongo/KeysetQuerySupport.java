package com.apargo.services.audit.infrastructure.mongo;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.bson.conversions.Bson;

import com.apargo.services.audit.application.query.PageCursor;
import com.apargo.services.audit.common.constant.MongoFields;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * Time-window and keyset (cursor) criteria shared by both readers. The sort matches the trailing
 * {@code occurredAt:-1, _id:-1} of every timeline index, so pages are served from the index.
 */
final class KeysetQuerySupport {

    static final Bson NEWEST_FIRST = Sorts.descending(MongoFields.OCCURRED_AT, MongoFields.ID);

    private KeysetQuerySupport() {
    }

    /** {@code from <= occurredAt < to}. */
    static Bson timeRange(Instant from, Instant to) {
        return Filters.and(
                Filters.gte(MongoFields.OCCURRED_AT, Date.from(from)),
                Filters.lt(MongoFields.OCCURRED_AT, Date.from(to)));
    }

    /** Items strictly after the cursor in NEWEST_FIRST order. */
    static Bson after(PageCursor cursor) {
        Date occurredAt = Date.from(cursor.occurredAt());
        return Filters.or(
                Filters.lt(MongoFields.OCCURRED_AT, occurredAt),
                Filters.and(Filters.eq(MongoFields.OCCURRED_AT, occurredAt),
                        Filters.lt(MongoFields.ID, cursor.eventId())));
    }

    static void addIfPresent(List<Bson> conditions, String field, Object value) {
        if (value != null) {
            conditions.add(Filters.eq(field, value));
        }
    }
}
