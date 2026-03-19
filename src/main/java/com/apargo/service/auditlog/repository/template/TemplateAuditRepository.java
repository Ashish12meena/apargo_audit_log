package com.apargo.service.auditlog.repository.template;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.apargo.service.auditlog.model.template.TemplateAuditEvent;

public interface TemplateAuditRepository  extends MongoRepository<TemplateAuditEvent, String> {

    boolean existsByEventId(String eventId);
    
}
