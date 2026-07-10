package com.apargo.service.auditlog.exception;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuditErrorResponse {
    private String code;
    private String message;
    private Instant timestamp;
    private String path;
}
