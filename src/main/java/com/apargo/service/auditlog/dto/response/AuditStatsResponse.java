package com.apargo.service.auditlog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class AuditStatsResponse {

    private Long organizationId;

    /** Effective range actually queried — echoes defaults back when the client omitted them. */
    private Instant fromDate;
    private Instant toDate;

    private List<String> groupBy;
    private String bucket;

    /** Sum of every bucket's count — saves the client from summing the array for a headline number. */
    private long totalEvents;

    private List<AuditStatsBucket> results;
}