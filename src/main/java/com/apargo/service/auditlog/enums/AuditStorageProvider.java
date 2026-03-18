package com.apargo.service.auditlog.enums;

/**
 * Storage provider that handled the file at the time of the event.
 * Mirrors StorageProviderType in the storage service — kept as a
 * separate enum so audit-log has zero dependency on storage-service code.
 */
public enum AuditStorageProvider {
    LOCAL,
    S3,
    AZURE_BLOB,
    GCS,
    MINIO,
    UNKNOWN
}   