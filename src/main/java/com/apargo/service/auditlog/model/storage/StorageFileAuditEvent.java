package com.apargo.service.auditlog.model.storage;



import com.apargo.service.auditlog.enums.AuditEventStatus;
import com.apargo.service.auditlog.enums.StorageAuditEventType;
import com.apargo.service.auditlog.enums.AuditStorageProvider;
import com.apargo.service.auditlog.enums.DeleteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "storage_file_audit")
@CompoundIndexes({
        @CompoundIndex(
                name = "idx_file_org_project_occurred",
                def = "{'orgId': 1, 'projectId': 1, 'occurredAt': -1}"
        ),
        @CompoundIndex(
                name = "idx_file_org_eventtype_occurred",
                def = "{'orgId': 1, 'eventType': 1, 'occurredAt': -1}"
        ),
        @CompoundIndex(
                name = "idx_file_storagekey_occurred",
                def = "{'storageKey': 1, 'occurredAt': -1}"
        )
})
public class StorageFileAuditEvent {

    // ── Identity ─────────────────────────────────────────────────────────────

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("eventId")
    private String eventId;

    // ── Classification ───────────────────────────────────────────────────────

    private StorageAuditEventType eventType;

    private AuditEventStatus eventStatus;

    // ── Who Did It ───────────────────────────────────────────────────────────

    /** Organisation that performed the action. Always present. */
    private Long orgId;

    /** Project that performed the action. Always present. */
    private Long projectId;

    /**
     * WhatsApp Business Account ID.
     * Present for upload events (from X-Waba-Id request header).
     * Null for delete events — deletes are internal/admin triggered.
     */
    private String wabaId;

    // ── File Identity ────────────────────────────────────────────────────────

    /**
     * Storage key: org-{id}/proj-{id}/{type}/{uuid}.{ext}
     * Present for single MEDIA_UPLOAD (success) and MEDIA_DELETE.
     * Null for batch events and bulk deletes.
     */
    private String storageKey;

    /**
     * Original client-provided filename.
     * Present for MEDIA_UPLOAD and MEDIA_DELETE.
     * Null for batch and bulk delete events.
     */
    private String originalFilename;

    /** MIME type. Present for upload events. Null for deletes. */
    private String mimeType;

    /**
     * File size in bytes.
     * For MEDIA_BULK_DELETE: total bytes freed (sum of all deleted files).
     */
    private Long fileSizeBytes;

    /** Storage backend. Null for quota and access events. */
    private AuditStorageProvider storageProvider;

    // ── Batch Summary (MEDIA_BATCH_UPLOAD only) ───────────────────────────────
    private Integer batchTotalFiles;

    private Integer batchSuccessCount;

    private Integer batchFailedCount;

    // ── Delete Context (MEDIA_DELETE, MEDIA_BULK_DELETE only) ────────────────

    /**
     * SOFT → record marked deleted, file may still exist in storage.
     * HARD → record and file both permanently removed.
     */
    @Field("deleteType")
    private DeleteType deleteType;

    /**
     * For MEDIA_BULK_DELETE: scope of deletion.
     * "PROJECT" → all files for one project deleted.
     * "ORG"     → all files across all projects of an org deleted.
     */
    private String bulkDeleteScope;

    /**
     * For MEDIA_BULK_DELETE: how many files were deleted in this bulk operation.
     */
    private Long bulkDeleteCount;

    // ── Failure Detail ───────────────────────────────────────────────────────

    private String errorMessage;

    // ── Timestamps ───────────────────────────────────────────────────────────

    private Instant occurredAt;

    private Instant recordedAt;
}