package com.apargo.service.auditlog.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * One grouped count in an {@link AuditStatsResponse}.
 * <p>
 * {@code dimensions} is a nested map rather than flat top-level fields
 * (e.g. {@code {"module": "X", "eventStatus": "Y"}}) so the shape stays
 * stable regardless of which/how many fields were grouped on — a client
 * doesn't need prior knowledge of the groupBy combination to parse the
 * response, it just reads whatever keys are present.
 */
@Data
@Builder
public class AuditStatsBucket {

    /** Populated only when the request specified a {@code bucket} (day/hour); null otherwise. */
    private String date;

    private Map<String, String> dimensions;

    private long count;
}