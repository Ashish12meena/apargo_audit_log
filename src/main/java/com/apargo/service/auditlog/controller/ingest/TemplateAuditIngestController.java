package com.apargo.service.auditlog.controller.ingest;

import com.apargo.service.auditlog.dto.request.ingest.template.TemplateAuditIngestRequest;
import com.apargo.service.auditlog.dto.request.ingest.template.TemplateSyncAuditIngestRequest;
import com.apargo.service.auditlog.router.TemplateAuditRouter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/audit/templates")
@RequiredArgsConstructor
@Slf4j
public class TemplateAuditIngestController {

    private final TemplateAuditRouter router;

    @PostMapping("/events")
    public ResponseEntity<Void> ingestTemplateEvent(
            @RequestBody @Valid TemplateAuditIngestRequest request) {

        log.info("Template audit event received: eventType={} eventId={}",
                request.getEventType(), request.getEventId());
        router.route(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/sync/events")
    public ResponseEntity<Void> ingestSyncEvent(
            @RequestBody @Valid TemplateSyncAuditIngestRequest request) {

        log.info("Template sync audit event received: eventType={} eventId={}",
                request.getEventType(), request.getEventId());
        router.routeSync(request);
        return ResponseEntity.accepted().build();
    }
}