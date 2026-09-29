package com.apargo.services.audit.infrastructure.mongo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.apargo.platform.contract.audit.AuditActorDto;
import com.apargo.platform.contract.audit.AuditChangeDto;
import com.apargo.platform.contract.audit.AuditChannel;
import com.apargo.platform.contract.audit.AuditEntityDto;
import com.apargo.platform.contract.audit.AuditErrorCategory;
import com.apargo.platform.contract.audit.AuditErrorDto;
import com.apargo.platform.contract.audit.AuditEventStatus;
import com.apargo.platform.contract.identity.ActorType;
import com.apargo.services.audit.common.constant.MongoFields;
import com.apargo.services.audit.common.constant.MongoFields.Nested;
import com.apargo.services.audit.domain.model.AccessActor;
import com.apargo.services.audit.domain.model.AccessLog;
import com.apargo.services.audit.domain.model.AuditLog;
import com.apargo.services.audit.domain.model.LogRecord;

/**
 * Maps log records to BSON and back. Field names are the contract's (see {@link MongoFields}).
 * Null optional fields are not written, matching the contract's {@code NON_NULL} serialization;
 * {@code changes} and {@code metadata} of audit logs are always written, even when empty.
 */
@Component
public class LogDocumentMapper {

    private static final Logger log = LoggerFactory.getLogger(LogDocumentMapper.class);

    public Document toDocument(LogRecord record) {
        return switch (record) {
            case AuditLog audit -> toDocument(audit);
            case AccessLog access -> toDocument(access);
        };
    }

    // ---- audit_logs ----------------------------------------------------------------------------

    private Document toDocument(AuditLog audit) {
        Document doc = new Document(MongoFields.ID, audit.eventId())
                .append(MongoFields.SCHEMA_VERSION, audit.schemaVersion())
                .append(MongoFields.SOURCE_SERVICE, audit.sourceService())
                .append(MongoFields.MODULE, audit.module())
                .append(MongoFields.EVENT_TYPE, audit.eventType())
                .append(MongoFields.STATUS, audit.status().name())
                .append(MongoFields.ORG_ID, audit.orgId());
        putIfPresent(doc, MongoFields.PROJECT_ID, audit.projectId());
        doc.append(MongoFields.ACTOR, actorDocument(audit.actor()));
        putIfPresent(doc, MongoFields.ENTITY, entityDocument(audit.entity()));
        doc.append(MongoFields.CHANGES, changesList(audit.changes()));
        doc.append(MongoFields.METADATA, BsonValues.toDocument(audit.metadata()));
        putIfPresent(doc, MongoFields.ERROR, errorDocument(audit.error()));
        putIfPresent(doc, MongoFields.CHANNEL, audit.channel() == null ? null : audit.channel().name());
        putIfPresent(doc, MongoFields.REQUEST_ID, audit.requestId());
        putIfPresent(doc, MongoFields.TRACE_ID, audit.traceId());
        putIfPresent(doc, MongoFields.IP, audit.ip());
        putIfPresent(doc, MongoFields.USER_AGENT, audit.userAgent());
        return appendTimes(doc, audit.occurredAt(), audit.recordedAt(), audit.archiveAt());
    }

    public AuditLog toAuditLog(Document doc) {
        return new AuditLog(
                doc.getString(MongoFields.ID),
                intValue(doc.get(MongoFields.SCHEMA_VERSION)),
                doc.getString(MongoFields.SOURCE_SERVICE),
                doc.getString(MongoFields.MODULE),
                doc.getString(MongoFields.EVENT_TYPE),
                enumValue(AuditEventStatus.class, doc.getString(MongoFields.STATUS)),
                longValue(doc.get(MongoFields.ORG_ID)),
                nullableLong(doc.get(MongoFields.PROJECT_ID)),
                toActor(doc.get(MongoFields.ACTOR, Document.class)),
                toEntity(doc.get(MongoFields.ENTITY, Document.class)),
                toChanges(doc.getList(MongoFields.CHANGES, Document.class)),
                BsonValues.toMap(doc.get(MongoFields.METADATA, Document.class)),
                toError(doc.get(MongoFields.ERROR, Document.class)),
                enumValue(AuditChannel.class, doc.getString(MongoFields.CHANNEL)),
                doc.getString(MongoFields.REQUEST_ID),
                doc.getString(MongoFields.TRACE_ID),
                doc.getString(MongoFields.IP),
                doc.getString(MongoFields.USER_AGENT),
                instant(doc, MongoFields.OCCURRED_AT),
                instant(doc, MongoFields.RECORDED_AT),
                instant(doc, MongoFields.ARCHIVE_AT));
    }

    private static Document actorDocument(AuditActorDto actor) {
        Document doc = new Document(Nested.TYPE, actor.type().name());
        putIfPresent(doc, Nested.ID, actor.id());
        putIfPresent(doc, Nested.NAME, actor.name());
        putIfPresent(doc, Nested.IMPERSONATOR_ID, actor.impersonatorId());
        return doc;
    }

    private static AuditActorDto toActor(Document doc) {
        if (doc == null) {
            return null;
        }
        return new AuditActorDto(enumValue(ActorType.class, doc.getString(Nested.TYPE)), doc.getString(Nested.ID),
                doc.getString(Nested.NAME), doc.getString(Nested.IMPERSONATOR_ID));
    }

    private static Document entityDocument(AuditEntityDto entity) {
        if (entity == null) {
            return null;
        }
        Document doc = new Document(Nested.TYPE, entity.type());
        putIfPresent(doc, Nested.ID, entity.id());
        putIfPresent(doc, Nested.NAME, entity.name());
        return doc;
    }

    private static AuditEntityDto toEntity(Document doc) {
        return doc == null ? null
                : new AuditEntityDto(doc.getString(Nested.TYPE), doc.getString(Nested.ID), doc.getString(Nested.NAME));
    }

    /** A null old/new value is not written; it reads back as null, which is the same value. */
    private static List<Document> changesList(List<AuditChangeDto> changes) {
        List<Document> list = new ArrayList<>(changes.size());
        for (AuditChangeDto change : changes) {
            Document doc = new Document(Nested.FIELD, change.field());
            putIfPresent(doc, Nested.OLD_VALUE, BsonValues.toBson(change.oldValue()));
            putIfPresent(doc, Nested.NEW_VALUE, BsonValues.toBson(change.newValue()));
            list.add(doc);
        }
        return list;
    }

    private static List<AuditChangeDto> toChanges(List<Document> docs) {
        if (docs == null) {
            return List.of();
        }
        List<AuditChangeDto> changes = new ArrayList<>(docs.size());
        for (Document doc : docs) {
            changes.add(new AuditChangeDto(doc.getString(Nested.FIELD),
                    BsonValues.fromBson(doc.get(Nested.OLD_VALUE)), BsonValues.fromBson(doc.get(Nested.NEW_VALUE))));
        }
        return changes;
    }

    private static Document errorDocument(AuditErrorDto error) {
        if (error == null) {
            return null;
        }
        Document doc = new Document();
        putIfPresent(doc, Nested.CATEGORY, error.category() == null ? null : error.category().name());
        putIfPresent(doc, Nested.CODE, error.code());
        putIfPresent(doc, Nested.MESSAGE, error.message());
        putIfPresent(doc, Nested.DETAILS, BsonValues.toDocument(error.details()));
        putIfPresent(doc, Nested.REFERENCE, error.reference());
        return doc;
    }

    private static AuditErrorDto toError(Document doc) {
        if (doc == null) {
            return null;
        }
        return new AuditErrorDto(enumValue(AuditErrorCategory.class, doc.getString(Nested.CATEGORY)),
                doc.getString(Nested.CODE), doc.getString(Nested.MESSAGE),
                BsonValues.toMap(doc.get(Nested.DETAILS, Document.class)), doc.getString(Nested.REFERENCE));
    }

    // ---- access_logs ---------------------------------------------------------------------------

    private Document toDocument(AccessLog access) {
        Document doc = new Document(MongoFields.ID, access.eventId())
                .append(MongoFields.SCHEMA_VERSION, access.schemaVersion())
                .append(MongoFields.SOURCE_SERVICE, access.sourceService())
                .append(MongoFields.EVENT_TYPE, access.eventType())
                .append(MongoFields.STATUS, access.status());
        putIfPresent(doc, MongoFields.ORG_ID, access.orgId());
        doc.append(MongoFields.ACTOR, accessActorDocument(access.actor()));
        putIfPresent(doc, MongoFields.AUTH_METHOD, access.authMethod());
        putIfPresent(doc, MongoFields.FAILURE_CODE, access.failureCode());
        putIfPresent(doc, MongoFields.RESOURCE, access.resource());
        putIfPresent(doc, MongoFields.IP, access.ip());
        putIfPresent(doc, MongoFields.USER_AGENT, access.userAgent());
        putIfPresent(doc, MongoFields.COUNTRY, access.country());
        putIfPresent(doc, MongoFields.METADATA, BsonValues.toDocument(access.metadata()));
        putIfPresent(doc, MongoFields.REQUEST_ID, access.requestId());
        putIfPresent(doc, MongoFields.TRACE_ID, access.traceId());
        return appendTimes(doc, access.occurredAt(), access.recordedAt(), access.archiveAt());
    }

    public AccessLog toAccessLog(Document doc) {
        Document actor = doc.get(MongoFields.ACTOR, Document.class);
        return new AccessLog(
                doc.getString(MongoFields.ID),
                intValue(doc.get(MongoFields.SCHEMA_VERSION)),
                doc.getString(MongoFields.SOURCE_SERVICE),
                doc.getString(MongoFields.EVENT_TYPE),
                doc.getString(MongoFields.STATUS),
                nullableLong(doc.get(MongoFields.ORG_ID)),
                actor == null ? null : new AccessActor(actor.getString(Nested.TYPE), actor.getString(Nested.ID),
                        actor.getString(Nested.NAME), actor.getString(Nested.EMAIL),
                        actor.getString(Nested.IMPERSONATOR_ID)),
                doc.getString(MongoFields.AUTH_METHOD),
                doc.getString(MongoFields.FAILURE_CODE),
                doc.getString(MongoFields.RESOURCE),
                doc.getString(MongoFields.IP),
                doc.getString(MongoFields.USER_AGENT),
                doc.getString(MongoFields.COUNTRY),
                BsonValues.toMap(doc.get(MongoFields.METADATA, Document.class)),
                doc.getString(MongoFields.REQUEST_ID),
                doc.getString(MongoFields.TRACE_ID),
                instant(doc, MongoFields.OCCURRED_AT),
                instant(doc, MongoFields.RECORDED_AT),
                instant(doc, MongoFields.ARCHIVE_AT));
    }

    private static Document accessActorDocument(AccessActor actor) {
        Document doc = new Document(Nested.TYPE, actor.type());
        putIfPresent(doc, Nested.ID, actor.id());
        putIfPresent(doc, Nested.NAME, actor.name());
        putIfPresent(doc, Nested.EMAIL, actor.email());
        putIfPresent(doc, Nested.IMPERSONATOR_ID, actor.impersonatorId());
        return doc;
    }

    // ---- helpers -------------------------------------------------------------------------------

    private static Document appendTimes(Document doc, Instant occurredAt, Instant recordedAt, Instant archiveAt) {
        return doc.append(MongoFields.OCCURRED_AT, Date.from(occurredAt))
                .append(MongoFields.RECORDED_AT, Date.from(recordedAt))
                .append(MongoFields.ARCHIVE_AT, Date.from(archiveAt));
    }

    private static void putIfPresent(Document doc, String key, Object value) {
        if (value != null) {
            doc.append(key, value);
        }
    }

    private static Instant instant(Document doc, String key) {
        Date date = doc.getDate(key);
        return date == null ? null : date.toInstant();
    }

    private static int intValue(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static long longValue(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static Long nullableLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }

    /** Unknown stored values (e.g. written by a newer contract) read as null instead of failing the query. */
    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            log.debug("Stored value '{}' is not a known {}", value, type.getSimpleName());
            return null;
        }
    }
}
