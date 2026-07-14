package com.apargo.service.auditlog.repository.read;

import com.apargo.service.auditlog.dto.request.AuditSearchRequest;
import com.apargo.service.auditlog.model.AuditLog;
import com.apargo.service.auditlog.util.AuditConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds a single Criteria query per request from whichever filters were
 * actually supplied — every filter is optional and additive (AND'd together).
 * Runs against the read-preferred MongoTemplate (see MongoReadWriteConfig)
 * so heavy filter/pagination queries never contend with the write path.
 */
@Repository
@RequiredArgsConstructor
public class AuditLogSearchRepositoryImpl implements AuditLogSearchRepository {

    private final MongoTemplate readMongoTemplate;

    @Override
    public Page<AuditLog> search(AuditSearchRequest f, Pageable pageable) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (f.getOrganizationId() != null) {
            criteriaList.add(Criteria.where(AuditConstants.FIELD_ORG_ID).is(f.getOrganizationId()));
        }
        if (f.getProjectId() != null) {
            criteriaList.add(Criteria.where(AuditConstants.FIELD_PROJECT_ID).is(f.getProjectId()));
        }

        if (f.getDevice() != null) {
            criteriaList.add(Criteria.where(AuditConstants.FIELD_DEVICE).is(f.getDevice()));
        }
        if (f.getUserId() != null) {
            criteriaList.add(Criteria.where(AuditConstants.FIELD_ACTOR_ID).is(f.getUserId()));
        }
        if (f.getModule() != null) {
            criteriaList.add(Criteria.where(AuditConstants.FIELD_MODULE).is(f.getModule()));
        }
        if (f.getAction() != null) {
            criteriaList.add(Criteria.where(AuditConstants.FIELD_EVENT_TYPE).is(f.getAction()));
        }
        if (f.getEntityType() != null) {
            criteriaList.add(Criteria.where(AuditConstants.FIELD_ENTITY_TYPE).is(f.getEntityType()));
        }
        if (f.getEntityId() != null) {
            criteriaList.add(Criteria.where(AuditConstants.FIELD_ENTITY_ID).is(f.getEntityId()));
        }
        if (f.getStatus() != null) {
            criteriaList.add(Criteria.where(AuditConstants.FIELD_EVENT_STATUS).is(f.getStatus()));
        }
        if (f.getFromDate() != null || f.getToDate() != null) {
            Criteria dateCriteria = Criteria.where(AuditConstants.FIELD_OCCURRED_AT);
            if (f.getFromDate() != null) dateCriteria = dateCriteria.gte(f.getFromDate());
            if (f.getToDate() != null) dateCriteria = dateCriteria.lte(f.getToDate());
            criteriaList.add(dateCriteria);
        }
        if (f.getSearch() != null && !f.getSearch().isBlank()) {
            // Free-text convenience filter across a couple of human-readable
            // fields. Keep this narrow — it's not a full-text index, just a
            // regex OR, so it should never be the only filter on a large
            // date range in production.
            String regex = ".*" + Pattern.quote(f.getSearch()) + ".*";
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where(AuditConstants.FIELD_ENTITY_TYPE).regex(regex, "i"),
                    Criteria.where(AuditConstants.FIELD_ERROR_MESSAGE).regex(regex, "i")
            ));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = readMongoTemplate.count(query, AuditLog.class);
        query.with(pageable);
        List<AuditLog> results = readMongoTemplate.find(query, AuditLog.class);

        return new PageImpl<>(results, pageable, total);
    }
}
