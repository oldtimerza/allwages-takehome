package com.allwage.clockin.model;

/**
 * Stable reasons for clock audit outcomes.
 */
public enum AuditReasonCode {
    CLOCK_ACCEPTED,
    CLOCK_PROCESSING_FAILED,
    REQUEST_MALFORMED,
    REQUEST_VALIDATION_FAILED
}
