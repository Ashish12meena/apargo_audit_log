package com.apargo.service.auditlog.controller.v1;

import com.apargo.service.auditlog.dto.request.AuditIngestRequest;
import com.apargo.service.auditlog.dto.response.AuditEventResponse;
import com.apargo.service.auditlog.service.command.AuditCommandService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Audit Log Ingest", description = "Synchronous API for writing audit log events directly, bypassing the Kafka pipeline")
public class AuditIngestController {

    private final AuditCommandService commandService;

    @PostMapping
    @Operation(summary = "Ingest an audit log event synchronously", description = "Writes a single audit log event immediately and returns the persisted event. Intended for callers that need a hard failure signal if the write fails, or that don't have a Kafka producer available. High-volume or fire-and-forget events should be published via Kafka instead (AuditKafkaConsumer) to avoid blocking on write latency.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Audit log event ingested and persisted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or incomplete ingest payload")
    })
    public ResponseEntity<AuditEventResponse> ingest(
            @Valid @RequestBody AuditIngestRequest request) {
        log.info("audit_ingest_rest_received module={} eventType={} orgId={}",
                request.getModule(), request.getEventType(), request.getOrgId());
        AuditEventResponse response = commandService.ingest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}