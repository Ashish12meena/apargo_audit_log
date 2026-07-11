package com.apargo.service.auditlog.repository.read;

import com.apargo.service.auditlog.dto.response.AuditStatsBucket;
import com.apargo.service.auditlog.model.AuditLog;
import com.apargo.service.auditlog.util.AuditConstants;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationExpression;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the grouped-count aggregation behind GET /api/v1/audit-logs/stats.
 * <p>
 * Uses {@code readMongoTemplate} (secondary-preferred, same as
 * {@link AuditLogSearchRepositoryImpl}) — this is a dashboard read path and
 * must never contend with the write path for primary node resources.
 * <p>
 * Pipeline shape: {@code $match} (org/project/date scope) → optional
 * {@code $addFields} (computes a synthetic {@code bucketDate} string from
 * {@code occurredAt} when a time bucket was requested) → {@code $group}
 * (by the whitelisted field(s), plus {@code bucketDate} if present) →
 * {@code $sort} by count descending.
 * <p>
 * {@code allowDiskUse(true)} is set because grouped counts over a wide date
 * range can spill past the 100MB in-memory aggregation limit once org
 * volume grows — cheap insurance now, before it becomes a production
 * surprise.
 */
@Repository
@RequiredArgsConstructor
public class AuditLogStatsRepositoryImpl implements AuditLogStatsRepository {

    private static final Map<String, String> DATE_FORMATS = Map.of(
            AuditConstants.BUCKET_DAY, "%Y-%m-%d",
            AuditConstants.BUCKET_HOUR, "%Y-%m-%dT%H:00:00"
    );

    private final MongoTemplate readMongoTemplate;

    @Override
    public List<AuditStatsBucket> aggregate(Long organizationId, Long projectId, Instant from, Instant to,
                                             List<String> groupByFields, String bucket) {

        List<AggregationOperation> stages = new ArrayList<>();
        stages.add(Aggregation.match(buildMatchCriteria(organizationId, projectId, from, to)));

        // The group stage groups by the whitelisted business fields, plus a
        // synthetic bucketDate field when time-bucketing was requested —
        // effectiveGroupFields is what _id actually looks like on each row.
        List<String> effectiveGroupFields = new ArrayList<>(groupByFields);
        if (bucket != null) {
            stages.add(buildBucketDateStage(bucket));
            effectiveGroupFields.add(AuditConstants.FIELD_BUCKET_DATE);
        }

        stages.add(buildGroupStage(effectiveGroupFields));
        stages.add(Aggregation.sort(Sort.Direction.DESC, AuditConstants.FIELD_COUNT));

        Aggregation aggregation = Aggregation.newAggregation(stages)
                .withOptions(AggregationOptions.builder().allowDiskUse(true).build());

        AggregationResults<Document> results =
                readMongoTemplate.aggregate(aggregation, AuditLog.class, Document.class);

        return results.getMappedResults().stream()
                .map(doc -> toBucket(doc, groupByFields, bucket))
                .toList();
    }

    private Criteria buildMatchCriteria(Long organizationId, Long projectId, Instant from, Instant to) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where(AuditConstants.FIELD_ORG_ID).is(organizationId));

        if (projectId != null) {
            criteria.add(Criteria.where(AuditConstants.FIELD_PROJECT_ID).is(projectId));
        }

        criteria.add(Criteria.where(AuditConstants.FIELD_OCCURRED_AT).gte(from).lte(to));

        return new Criteria().andOperator(criteria.toArray(new Criteria[0]));
    }

    private AddFieldsOperation buildBucketDateStage(String bucket) {
        AggregationExpression bucketDateExpr = DateOperators.DateToString
                .dateOf(AuditConstants.FIELD_OCCURRED_AT)
                .toString(DATE_FORMATS.get(bucket));

        return Aggregation.addFields()
                .addField(AuditConstants.FIELD_BUCKET_DATE)
                .withValue(bucketDateExpr)
                .build();
    }

    /**
     * Hand-built $group stage instead of {@code Aggregation.group(String...)}.
     * <p>
     * That shorthand renders {@code _id} differently depending on argument
     * count: with 2+ fields it's a nested document
     * ({@code {"module": "X", "eventStatus": "Y"}}), but with exactly one
     * field it collapses to a flat scalar ({@code _id: "Y"}) instead of
     * {@code {"eventStatus": "Y"}}. {@link #toBucket} always expects a
     * nested document, so a single-field groupBy (e.g. {@code groupBy=eventStatus})
     * threw a ClassCastException trying to read a Document out of a String.
     * Building the stage manually keeps {@code _id} a nested document
     * unconditionally, so the output shape never depends on how many
     * fields were grouped on.
     */
    private AggregationOperation buildGroupStage(List<String> fields) {
        Document idExpression = new Document();
        for (String field : fields) {
            idExpression.append(field, "$" + field);
        }

        Document groupStage = new Document("_id", idExpression)
                .append(AuditConstants.FIELD_COUNT, new Document("$sum", 1));

        return context -> new Document("$group", groupStage);
    }

    private AuditStatsBucket toBucket(Document doc, List<String> groupByFields, String bucket) {
        Document id = doc.get("_id", Document.class);

        Map<String, String> dimensions = new LinkedHashMap<>();
        for (String field : groupByFields) {
            Object value = id.get(field);
            dimensions.put(field, value != null ? value.toString() : null);
        }

        String date = null;
        if (bucket != null) {
            Object bucketValue = id.get(AuditConstants.FIELD_BUCKET_DATE);
            date = bucketValue != null ? bucketValue.toString() : null;
        }

        return AuditStatsBucket.builder()
                .date(date)
                .dimensions(dimensions)
                .count(doc.getInteger(AuditConstants.FIELD_COUNT, 0))
                .build();
    }
}