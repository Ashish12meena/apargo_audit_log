package com.apargo.services.audit.application.ingest;

import com.apargo.services.audit.common.enums.DeadLetterReason;

/** A record that will never be stored as-is, and why. */
public record DeadLetter(InboundEvent source, DeadLetterReason reason, String detail) {
}
