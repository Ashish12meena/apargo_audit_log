package com.apargo.services.audit.infrastructure.mongo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.port.out.AuditLogReader;
import com.apargo.services.audit.application.query.AuditLogFilter;
import com.apargo.services.audit.application.query.PageCursor;
import com.apargo.services.audit.application.query.Slice;
import com.apargo.services.audit.application.query.StatsQuery;
import com.apargo.services.audit.application.query.StatsRow;
import com.apargo.services.audit.common.constant.MongoCollections;
import com.apargo.services.audit.common.constant.MongoFields;
import com.apargo.services.audit.common.enums.StatsDimension;
import com.apargo.services.audit.domain.model.AuditLog;
import com.apargo.services.audit.infrastructure.config.AuditProperties;
import com.apargo.services.audit.infrastructure.config.MongoConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/** Reads {@code audit_logs} from the read client. Every query carries {@code maxTimeMS}. */
@Component
public class MongoAuditLogReader implements AuditLogReader {

    private static final String COUNT = "count";
    private static final String PERIOD = "period";

    private final MongoCollection<Document> collection;
    private final LogDocumentMapper mapper;
    private final long maxTimeMs;

    public MongoAuditLogReader(@Qualifier(MongoConfig.READ_DATABASE) MongoDatabase database,
                               LogDocumentMapper mapper, AuditProperties properties) {
        this.collection = database.getCollection(MongoCollections.AUDIT_LOGS);
        this.mapper = mapper;
        this.maxTimeMs = properties.mongo().queryMaxTime().toMillis();
    }

    @Override
    public Slice<AuditLog> search(AuditLogFilter filter, PageCursor after, int limit) {
        List<Bson> conditions = new ArrayList<>();
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.ORG_ID, filter.orgId());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.PROJECT_ID, filter.projectId());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.MODULE, filter.module());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.EVENT_TYPE, filter.eventType());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.STATUS, filter.status());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.ACTOR_ID, filter.actorId());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.ENTITY_TYPE, filter.entityType());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.ENTITY_ID, filter.entityId());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.SOURCE_SERVICE, filter.sourceService());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.CHANNEL, filter.channel());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.ERROR_CODE, filter.errorCode());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.TRACE_ID, filter.traceId());
        conditions.add(KeysetQuerySupport.timeRange(filter.from(), filter.to()));
        if (after != null) {
            conditions.add(KeysetQuerySupport.after(after));
        }

        List<Document> documents = MongoErrorClassifier.read("audit log search", () -> collection
                .find(Filters.and(conditions))
                .sort(KeysetQuerySupport.NEWEST_FIRST)
                .limit(limit + 1)
                .maxTime(maxTimeMs, TimeUnit.MILLISECONDS)
                .into(new ArrayList<>()));

        boolean hasMore = documents.size() > limit;
        List<AuditLog> items = documents.stream().limit(limit).map(mapper::toAuditLog).toList();
        return new Slice<>(items, hasMore);
    }

    @Override
    public Optional<AuditLog> findById(String eventId, Long orgId) {
        List<Bson> conditions = new ArrayList<>();
        conditions.add(Filters.eq(MongoFields.ID, eventId));
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.ORG_ID, orgId);
        Document document = MongoErrorClassifier.read("audit log lookup", () -> collection
                .find(Filters.and(conditions))
                .maxTime(maxTimeMs, TimeUnit.MILLISECONDS)
                .first());
        return Optional.ofNullable(document).map(mapper::toAuditLog);
    }

    @Override
    public List<StatsRow> stats(StatsQuery query, int maxRows) {
        List<Bson> match = new ArrayList<>();
        match.add(Filters.eq(MongoFields.ORG_ID, query.orgId()));
        KeysetQuerySupport.addIfPresent(match, MongoFields.PROJECT_ID, query.projectId());
        match.add(KeysetQuerySupport.timeRange(query.from(), query.to()));

        Document groupId = new Document();
        for (StatsDimension dimension : query.groupBy()) {
            groupId.append(dimension.apiName(), "$" + fieldPath(dimension));
        }
        if (query.bucket() != null) {
            groupId.append(PERIOD, new Document("$dateToString", new Document("format", query.bucket().dateFormat())
                    .append("date", "$" + MongoFields.OCCURRED_AT)
                    .append("timezone", "UTC")));
        }

        List<Bson> pipeline = List.of(
                Aggregates.match(Filters.and(match)),
                new Document("$group", new Document("_id", groupId).append(COUNT, new Document("$sum", 1))),
                Aggregates.sort(Sorts.descending(COUNT)),
                Aggregates.limit(maxRows));

        List<Document> results = MongoErrorClassifier.read("audit stats", () -> collection
                .aggregate(pipeline)
                .allowDiskUse(true)
                .maxTime(maxTimeMs, TimeUnit.MILLISECONDS)
                .into(new ArrayList<>()));

        List<StatsRow> rows = new ArrayList<>(results.size());
        for (Document result : results) {
            Document id = result.get("_id", Document.class);
            Map<String, String> dimensions = new LinkedHashMap<>();
            for (StatsDimension dimension : query.groupBy()) {
                Object value = id == null ? null : id.get(dimension.apiName());
                dimensions.put(dimension.apiName(), value == null ? null : value.toString());
            }
            String period = id == null ? null : id.getString(PERIOD);
            Number count = result.get(COUNT, Number.class);
            rows.add(new StatsRow(dimensions, period, count == null ? 0 : count.longValue()));
        }
        return rows;
    }

    private static String fieldPath(StatsDimension dimension) {
        return switch (dimension) {
            case MODULE -> MongoFields.MODULE;
            case EVENT_TYPE -> MongoFields.EVENT_TYPE;
            case STATUS -> MongoFields.STATUS;
            case CHANNEL -> MongoFields.CHANNEL;
            case SOURCE_SERVICE -> MongoFields.SOURCE_SERVICE;
            case ACTOR_TYPE -> MongoFields.ACTOR_TYPE;
            case PROJECT_ID -> MongoFields.PROJECT_ID;
        };
    }
}
