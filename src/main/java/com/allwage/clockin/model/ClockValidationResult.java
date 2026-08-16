package com.allwage.clockin.model;

import java.util.Objects;

/**
 * The immutable decision made when a clock attempt is processed.
 */
public record ClockValidationResult(
    Decision decision,
    Reason reason,
    String siteId,
    String geofenceId
) {

    /**
     * Creates a validation result with a decision and stable reason.
     */
    public ClockValidationResult {
        Objects.requireNonNull(decision, "Clock validation decision is required");
        Objects.requireNonNull(reason, "Clock validation reason is required");
        if (decision == Decision.ACCEPTED && reason != Reason.ACCEPTED) {
            throw new IllegalArgumentException("Accepted clocks must use the accepted validation reason");
        }
        if (decision == Decision.REJECTED && reason == Reason.ACCEPTED) {
            throw new IllegalArgumentException("Rejected clocks must use a rejection validation reason");
        }
    }

    /**
     * Clock processing decisions.
     */
    public enum Decision {
        ACCEPTED,
        REJECTED
    }

    /**
     * Stable reasons for a clock validation decision.
     */
    public enum Reason {
        ACCEPTED,
        EMPLOYEE_NOT_FOUND,
        NO_SITE_ASSIGNMENT,
        OUTSIDE_GEOFENCE,
        AMBIGUOUS_SITE,
        APPROVAL_REQUIRED
    }
}
