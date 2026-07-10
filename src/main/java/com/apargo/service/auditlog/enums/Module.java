package com.apargo.service.auditlog.enums;

/**
 * Business/microservice domain that produced the audit event.
 * Used to partition eventType constants and as a primary query filter.
 */
public enum Module {
    TEMPLATE,
    MESSAGING_CAMPAIGN,
    MESSAGING_DISPATCH,
    MESSAGING_CONVERSATION,
    STORAGE,
    WALLET,
    USERS,
    PROJECTS,
    PLANS,
    WABA
}
