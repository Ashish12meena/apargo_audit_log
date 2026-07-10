package com.apargo.service.auditlog.controller.v1;



import com.apargo.service.auditlog.dto.request.AuditIngestRequest;
import com.apargo.service.auditlog.dto.response.AuditEventResponse;
import com.apargo.service.auditlog.service.command.AuditCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Synchronous ingest path for services that want an immediate ack (e.g. a
 * request that must fail loudly if the audit write fails, or a service that
 * doesn't have a Kafka producer wired up). High-volume/fire-and-forget
 * events should prefer the Kafka path (AuditKafkaConsumer) so producers
 * aren't blocked on this service's write latency.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Slf4j
public class AuditIngestController {

    private final AuditCommandService commandService;

    @PostMapping
    public ResponseEntity<AuditEventResponse> ingest(@Valid @RequestBody AuditIngestRequest request) {
        log.info("audit_ingest_rest_received module={} eventType={} orgId={}",
                request.getModule(), request.getEventType(), request.getOrgId());
        AuditEventResponse response = commandService.ingest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
