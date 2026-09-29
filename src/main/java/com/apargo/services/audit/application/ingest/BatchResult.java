package com.apargo.services.audit.application.ingest;

/** What happened to one polled batch. */
public record BatchResult(int received, int stored, int duplicates, int deadLettered) {
}
