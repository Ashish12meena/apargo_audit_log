package com.apargo.services.audit.infrastructure.mongo;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

/**
 * Converts the free-form values of {@code metadata}, {@code changes[]} and {@code error.details}
 * between JSON-decoded Java values and BSON, in both directions. Only plain JSON-like types are
 * produced on read, so the API can serialize them directly.
 */
final class BsonValues {

    private BsonValues() {
    }

    static Object toBson(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Integer
                || value instanceof Long || value instanceof Double || value instanceof Date) {
            return value;
        }
        if (value instanceof BigInteger big) {
            return big.bitLength() < Long.SIZE ? big.longValue() : big.toString();
        }
        if (value instanceof BigDecimal decimal) {
            return new Decimal128(decimal);
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof Instant instant) {
            return Date.from(instant);
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        if (value instanceof Map<?, ?> map) {
            Document document = new Document();
            map.forEach((k, v) -> document.append(String.valueOf(k), toBson(v)));
            return document;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> list = new ArrayList<>(collection.size());
            collection.forEach(v -> list.add(toBson(v)));
            return list;
        }
        return value.toString();
    }

    static Object fromBson(Object value) {
        if (value instanceof Date date) {
            return date.toInstant();
        }
        if (value instanceof Decimal128 decimal) {
            return decimal.bigDecimalValue();
        }
        if (value instanceof ObjectId id) {
            return id.toHexString();
        }
        if (value instanceof Map<?, ?> map) {
            return toMap(map);
        }
        if (value instanceof Collection<?> collection) {
            List<Object> list = new ArrayList<>(collection.size());
            collection.forEach(v -> list.add(fromBson(v)));
            return list;
        }
        return value;
    }

    static Map<String, Object> toMap(Map<?, ?> source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        source.forEach((k, v) -> map.put(String.valueOf(k), fromBson(v)));
        return map;
    }

    static Document toDocument(Map<String, ?> source) {
        return source == null ? null : (Document) toBson(source);
    }
}
