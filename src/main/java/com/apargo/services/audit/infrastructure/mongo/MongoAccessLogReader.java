package com.apargo.services.audit.infrastructure.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.port.out.AccessLogReader;
import com.apargo.services.audit.application.query.AccessLogFilter;
import com.apargo.services.audit.application.query.PageCursor;
import com.apargo.services.audit.application.query.Slice;
import com.apargo.services.audit.common.constant.MongoCollections;
import com.apargo.services.audit.common.constant.MongoFields;
import com.apargo.services.audit.domain.model.AccessLog;
import com.apargo.services.audit.infrastructure.config.AuditProperties;
import com.apargo.services.audit.infrastructure.config.MongoConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

/** Reads {@code access_logs} from the read client. Every query carries {@code maxTimeMS}. */
@Component
public class MongoAccessLogReader implements AccessLogReader {

    private final MongoCollection<Document> collection;
    private final LogDocumentMapper mapper;
    private final long maxTimeMs;

    public MongoAccessLogReader(@Qualifier(MongoConfig.READ_DATABASE) MongoDatabase database,
                                LogDocumentMapper mapper, AuditProperties properties) {
        this.collection = database.getCollection(MongoCollections.ACCESS_LOGS);
        this.mapper = mapper;
        this.maxTimeMs = properties.mongo().queryMaxTime().toMillis();
    }

    @Override
    public Slice<AccessLog> search(AccessLogFilter filter, PageCursor after, int limit) {
        List<Bson> conditions = new ArrayList<>();
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.ORG_ID, filter.orgId());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.EVENT_TYPE, filter.eventType());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.STATUS, filter.status());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.ACTOR_ID, filter.actorId());
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.TRACE_ID, filter.traceId());
        conditions.add(KeysetQuerySupport.timeRange(filter.from(), filter.to()));
        if (after != null) {
            conditions.add(KeysetQuerySupport.after(after));
        }

        List<Document> documents = MongoErrorClassifier.read("access log search", () -> collection
                .find(Filters.and(conditions))
                .sort(KeysetQuerySupport.NEWEST_FIRST)
                .limit(limit + 1)
                .maxTime(maxTimeMs, TimeUnit.MILLISECONDS)
                .into(new ArrayList<>()));

        boolean hasMore = documents.size() > limit;
        List<AccessLog> items = documents.stream().limit(limit).map(mapper::toAccessLog).toList();
        return new Slice<>(items, hasMore);
    }

    @Override
    public Optional<AccessLog> findById(String eventId, Long orgId) {
        List<Bson> conditions = new ArrayList<>();
        conditions.add(Filters.eq(MongoFields.ID, eventId));
        KeysetQuerySupport.addIfPresent(conditions, MongoFields.ORG_ID, orgId);
        Document document = MongoErrorClassifier.read("access log lookup", () -> collection
                .find(Filters.and(conditions))
                .maxTime(maxTimeMs, TimeUnit.MILLISECONDS)
                .first());
        return Optional.ofNullable(document).map(mapper::toAccessLog);
    }
}
