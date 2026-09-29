package com.apargo.services.audit.infrastructure.mongo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.apargo.services.audit.common.constant.MongoCollections;
import com.apargo.services.audit.infrastructure.config.AuditProperties;
import com.apargo.services.audit.infrastructure.config.MongoConfig;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ValidationAction;
import com.mongodb.client.model.ValidationLevel;
import com.mongodb.client.model.ValidationOptions;

/**
 * Applies {@code db/mongo/*.json} (collection, validator, indexes) before any listener starts.
 * Idempotent: creates what is missing, keeps validators in sync, and fails startup if an existing
 * index has the same name but a different definition (never silently drops or rebuilds one).
 * <p>
 * Needs a user allowed to create collections and indexes; with {@code audit.mongo.manage-schema=false}
 * a DBA applies the same files with {@code scripts/apply-mongo-schema.js} instead.
 */
@Component
@ConditionalOnProperty(prefix = "audit.mongo", name = "manage-schema", havingValue = "true", matchIfMissing = true)
public class MongoSchemaInitializer implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(MongoSchemaInitializer.class);

    private static final int NAMESPACE_EXISTS = 48;
    private static final Set<Integer> INDEX_CONFLICT_CODES = Set.of(85, 86); // IndexOptionsConflict, IndexKeySpecsConflict

    private final MongoDatabase database;
    private final AuditProperties properties;

    public MongoSchemaInitializer(@Qualifier(MongoConfig.WRITE_DATABASE) MongoDatabase database,
                                  AuditProperties properties) {
        this.database = database;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> existing = database.listCollectionNames().into(new ArrayList<>());
        for (String resource : MongoCollections.DEFINITION_RESOURCES) {
            apply(load(resource), existing);
        }
        log.info("mongo_schema_applied database={} collections={}", database.getName(),
                MongoCollections.DEFINITION_RESOURCES.size());
    }

    private void apply(Document definition, List<String> existing) {
        String name = definition.getString("collection");
        Document validator = definition.get("validator", Document.class);
        ValidationLevel level = ValidationLevel.fromString(definition.getString("validationLevel"));
        ValidationAction action = ValidationAction.fromString(definition.getString("validationAction"));

        if (existing.contains(name)) {
            database.runCommand(new Document("collMod", name)
                    .append("validator", validator)
                    .append("validationLevel", level.getValue())
                    .append("validationAction", action.getValue()));
        } else {
            create(name, validator, level, action);
        }

        for (Document index : definition.getList("indexes", Document.class)) {
            createIndex(name, index);
        }
    }

    private void create(String name, Document validator, ValidationLevel level, ValidationAction action) {
        CreateCollectionOptions options = new CreateCollectionOptions().validationOptions(new ValidationOptions()
                .validator(validator).validationLevel(level).validationAction(action));
        AuditProperties.Mongo mongo = properties.mongo();
        if (mongo.compressionEnabled()) {
            options.storageEngineOptions(new Document("wiredTiger",
                    new Document("configString", "block_compressor=" + mongo.blockCompressor())));
        }
        try {
            database.createCollection(name, options);
            log.info("mongo_collection_created name={} compressor={}", name, mongo.blockCompressor());
        } catch (MongoCommandException e) {
            if (e.getErrorCode() != NAMESPACE_EXISTS) {
                throw e;
            }
        }
    }

    private void createIndex(String collection, Document index) {
        String indexName = index.getString("name");
        IndexOptions options = new IndexOptions().name(indexName);
        Document partial = index.get("partialFilterExpression", Document.class);
        if (partial != null) {
            options.partialFilterExpression(partial);
        }
        try {
            database.getCollection(collection).createIndex(index.get("key", Document.class), options);
        } catch (MongoCommandException e) {
            if (INDEX_CONFLICT_CODES.contains(e.getErrorCode())) {
                throw new IllegalStateException("Index " + collection + "." + indexName
                        + " exists with a different definition; reconcile it manually before starting", e);
            }
            throw e;
        }
    }

    private static Document load(String resource) {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return Document.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + resource, e);
        }
    }
}
