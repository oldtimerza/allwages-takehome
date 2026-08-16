package com.allwage.clockin.model;

/**
 * Stable reasons for clock audit outcomes.
 */
public enum AuditReasonCode {
    CLOCK_ACCEPTED,
    EMPLOYEE_NOT_FOUND,
    NO_SITE_ASSIGNMENT,
    GEOFENCE_REJECTED,
    AMBIGUOUS_SITE,
    APPROVAL_REQUIRED,
    CLOCK_PROCESSING_FAILED,
    REQUEST_MALFORMED,
    REQUEST_VALIDATION_FAILED
}
