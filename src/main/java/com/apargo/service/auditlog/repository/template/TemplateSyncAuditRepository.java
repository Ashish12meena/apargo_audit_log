package com.apargo.service.auditlog.repository.template;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.apargo.service.auditlog.model.template.TemplateSyncAuditEvent;

public interface TemplateSyncAuditRepository extends MongoRepository<TemplateSyncAuditEvent, String>  {

    boolean existsByEventId(String eventId);
    
}
