package com.allwage.clockin.model;

import java.util.List;

/**
 * Optional rule overrides. A null field inherits from the next broader scope.
 */
public record ValidationRules(
    Integer toleranceMeters,
    Boolean approvalRequired,
    List<StrictModeHours> strictModeHours
) {

    /**
     * Creates an override set with a non-negative optional tolerance.
     */
    public ValidationRules {
        if (toleranceMeters != null && toleranceMeters < 0) {
            throw new IllegalArgumentException("Geofence tolerance cannot be negative");
        }
        strictModeHours = strictModeHours == null ? null : List.copyOf(strictModeHours);
    }
}
