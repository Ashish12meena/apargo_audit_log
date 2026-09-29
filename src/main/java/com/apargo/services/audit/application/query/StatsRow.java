package com.apargo.services.audit.application.query;

import java.util.Map;

/** One group: its dimension values (by API name), its time bucket label if any, and the count. */
public record StatsRow(Map<String, String> dimensions, String period, long count) {
}
