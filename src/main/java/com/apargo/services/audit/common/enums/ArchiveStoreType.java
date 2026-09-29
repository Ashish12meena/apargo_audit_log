package com.apargo.services.audit.common.enums;

/** Cold-storage implementation. NONE keeps everything in MongoDB and never deletes. */
public enum ArchiveStoreType {
    NONE,
    /** Local filesystem; development and tests only. */
    LOCAL
}
