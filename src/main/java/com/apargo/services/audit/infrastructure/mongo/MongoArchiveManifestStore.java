package com.apargo.services.audit.infrastructure.mongo;

import java.time.Instant;
import java.util.Date;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.application.port.out.ArchiveManifestStore;
import com.apargo.services.audit.common.constant.MongoCollections;
import com.apargo.services.audit.common.constant.MongoFields;
import com.apargo.services.audit.common.constant.MongoFields.Manifest;
import com.apargo.services.audit.common.enums.ArchiveStatus;
import com.apargo.services.audit.domain.model.ArchiveManifest;
import com.apargo.services.audit.infrastructure.config.MongoConfig;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;

/** Persists {@code archive_manifests}. */
@Component
public class MongoArchiveManifestStore implements ArchiveManifestStore {

    private static final ReplaceOptions UPSERT = new ReplaceOptions().upsert(true);

    private final MongoCollection<Document> collection;

    public MongoArchiveManifestStore(@Qualifier(MongoConfig.WRITE_DATABASE) MongoDatabase database) {
        this.collection = database.getCollection(MongoCollections.ARCHIVE_MANIFESTS);
    }

    @Override
    public void save(ArchiveManifest manifest) {
        Document doc = new Document(MongoFields.ID, manifest.id())
                .append(Manifest.COLLECTION, manifest.collection())
                .append(Manifest.RUN_ID, manifest.runId())
                .append(Manifest.DT, manifest.dt())
                .append(Manifest.OBJECT_KEY, manifest.objectKey())
                .append(Manifest.DOC_COUNT, manifest.docCount())
                .append(Manifest.BYTES, manifest.bytes())
                .append(Manifest.STATUS, manifest.status().name())
                .append(Manifest.EXPORTED_AT, Date.from(manifest.exportedAt()));
        appendIfPresent(doc, Manifest.SHA256, manifest.sha256());
        appendIfPresent(doc, Manifest.FIRST_ID, manifest.firstId());
        appendIfPresent(doc, Manifest.LAST_ID, manifest.lastId());
        appendIfPresent(doc, Manifest.MIN_OCCURRED_AT, toDate(manifest.minOccurredAt()));
        appendIfPresent(doc, Manifest.MAX_OCCURRED_AT, toDate(manifest.maxOccurredAt()));
        appendIfPresent(doc, Manifest.DELETED_AT, toDate(manifest.deletedAt()));
        appendIfPresent(doc, Manifest.ERROR, manifest.error());
        MongoErrorClassifier.write("manifest save", () ->
                collection.replaceOne(Filters.eq(MongoFields.ID, manifest.id()), doc, UPSERT));
    }

    @Override
    public void markDeleted(String manifestId, Instant deletedAt) {
        MongoErrorClassifier.write("manifest update", () -> collection.updateOne(
                Filters.eq(MongoFields.ID, manifestId),
                Updates.combine(Updates.set(Manifest.STATUS, ArchiveStatus.DELETED.name()),
                        Updates.set(Manifest.DELETED_AT, Date.from(deletedAt)))));
    }

    @Override
    public void markFailed(String manifestId, String error) {
        MongoErrorClassifier.write("manifest update", () -> collection.updateOne(
                Filters.eq(MongoFields.ID, manifestId),
                Updates.combine(Updates.set(Manifest.STATUS, ArchiveStatus.FAILED.name()),
                        Updates.set(Manifest.ERROR, error))));
    }

    private static void appendIfPresent(Document doc, String key, Object value) {
        if (value != null) {
            doc.append(key, value);
        }
    }

    private static Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }
}
