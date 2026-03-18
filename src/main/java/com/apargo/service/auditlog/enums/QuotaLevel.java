package com.apargo.service.auditlog.enums;

/**
 * Indicates which level the quota event applies to.
 * Used in QUOTA_PROVISIONED, QUOTA_UPDATED, QUOTA_EXCEEDED events.
 */
public enum QuotaLevel {
    ORG,
    PROJECT
}