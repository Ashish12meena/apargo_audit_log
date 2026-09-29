package com.apargo.services.audit.infrastructure.mongo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.port.out.ArchivableLogStore;
import com.apargo.services.audit.common.constant.MongoFields;
import com.apargo.services.audit.common.enums.LogStream;
import com.apargo.services.audit.infrastructure.config.MongoConfig;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * Archive-side access to the hot collections. Uses the write client (primary), so the documents
 * it exports are exactly the ones it deletes.
 */
@Component
public class MongoArchivableLogStore implements ArchivableLogStore {

    /** Extended JSON keeps BSON types (dates, int64, decimals) in cold storage. */
    private static final JsonWriterSettings EXTENDED_JSON = JsonWriterSettings.builder()
            .outputMode(JsonMode.EXTENDED).build();

    private final MongoDatabase database;

    public MongoArchivableLogStore(@Qualifier(MongoConfig.WRITE_DATABASE) MongoDatabase database) {
        this.database = database;
    }

    @Override
    public List<ArchiveRecord> fetchArchivable(LogStream stream, Instant cutoff, int limit) {
        List<Document> documents = MongoErrorClassifier.write("archive fetch " + stream.collection(), () -> database
                .getCollection(stream.collection())
                .find(Filters.lt(MongoFields.ARCHIVE_AT, Date.from(cutoff)))
                .sort(Sorts.ascending(MongoFields.ARCHIVE_AT, MongoFields.ID))
                .limit(limit)
                .into(new ArrayList<>()));
        List<ArchiveRecord> records = new ArrayList<>(documents.size());
        for (Document document : documents) {
            records.add(new ArchiveRecord(document.getString(MongoFields.ID),
                    document.getDate(MongoFields.OCCURRED_AT).toInstant(), document.toJson(EXTENDED_JSON)));
        }
        return records;
    }

    @Override
    public long deleteArchived(LogStream stream, List<String> eventIds, Instant cutoff) {
        if (eventIds.isEmpty()) {
            return 0;
        }
        return MongoErrorClassifier.write("archive delete " + stream.collection(), () -> database
                .getCollection(stream.collection())
                .deleteMany(Filters.and(
                        Filters.in(MongoFields.ID, eventIds),
                        Filters.lt(MongoFields.ARCHIVE_AT, Date.from(cutoff))))
                .getDeletedCount());
    }
}
