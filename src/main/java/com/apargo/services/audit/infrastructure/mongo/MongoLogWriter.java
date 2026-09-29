package com.apargo.services.audit.infrastructure.mongo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.port.out.LogWriter;
import com.apargo.services.audit.application.port.out.TransientFailureException;
import com.apargo.services.audit.common.constant.MongoFields;
import com.apargo.services.audit.common.enums.LogStream;
import com.apargo.services.audit.domain.model.LogRecord;
import com.apargo.services.audit.infrastructure.config.MongoConfig;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoException;
import com.mongodb.bulk.BulkWriteError;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;

/**
 * One unordered {@code insertMany} per batch. Unordered means one bad document never stops the
 * others; {@code _id = eventId} makes every retry and redelivery idempotent.
 */
@Component
public class MongoLogWriter implements LogWriter {

    private static final InsertManyOptions UNORDERED = new InsertManyOptions().ordered(false);

    private final MongoDatabase database;
    private final LogDocumentMapper mapper;

    public MongoLogWriter(@Qualifier(MongoConfig.WRITE_DATABASE) MongoDatabase database, LogDocumentMapper mapper) {
        this.database = database;
        this.mapper = mapper;
    }

    @Override
    public WriteOutcome write(LogStream stream, List<? extends LogRecord> records) {
        if (records.isEmpty()) {
            return WriteOutcome.empty();
        }
        List<Document> documents = new ArrayList<>(records.size());
        records.forEach(record -> documents.add(mapper.toDocument(record)));

        try {
            database.getCollection(stream.collection()).insertMany(documents, UNORDERED);
            return new WriteOutcome(documents.size(), 0, List.of());
        } catch (MongoBulkWriteException e) {
            return partialOutcome(stream, documents, e);
        } catch (MongoException e) {
            throw new TransientFailureException(
                    "insert into " + stream.collection() + " failed: " + MongoErrorClassifier.describe(e), e);
        }
    }

    /**
     * With an unordered insert every document without a write error was stored. A write-concern
     * error means durability is unknown, so the whole batch is retried (duplicates resolve on retry).
     */
    private static WriteOutcome partialOutcome(LogStream stream, List<Document> documents, MongoBulkWriteException e) {
        if (e.getWriteConcernError() != null) {
            throw new TransientFailureException("insert into " + stream.collection()
                    + " not acknowledged by a majority: " + e.getWriteConcernError().getMessage(), e);
        }
        int duplicates = 0;
        List<RejectedRecord> rejected = new ArrayList<>();
        for (BulkWriteError error : e.getWriteErrors()) {
            if (error.getCode() == MongoErrorClassifier.DUPLICATE_KEY) {
                duplicates++;
            } else if (MongoErrorClassifier.isTransientCode(error.getCode())) {
                throw new TransientFailureException("insert into " + stream.collection()
                        + " hit a transient error code=" + error.getCode(), e);
            } else {
                String eventId = documents.get(error.getIndex()).getString(MongoFields.ID);
                rejected.add(new RejectedRecord(eventId, error.getCode(),
                        MongoErrorClassifier.truncate(error.getMessage())));
            }
        }
        int stored = documents.size() - duplicates - rejected.size();
        return new WriteOutcome(stored, duplicates, rejected);
    }
}
