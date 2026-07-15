package com.apargo.service.auditlog.enums;

/**
 * Business/microservice domain that produced the audit event.
 * Used to partition eventType constants and as a primary query filter.
 */
public enum Module {
    ORGANIZATION,
    TEMPLATE,
    MESSAGING,
    STORAGE,
    WALLET,
    USERS,
    PROJECTS,
    PLANS,
    WABA
}
