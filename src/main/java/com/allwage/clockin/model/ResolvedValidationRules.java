package com.allwage.clockin.model;

import java.util.List;

/**
 * Validation settings after applying employee, team, site, and system defaults.
 */
public record ResolvedValidationRules(
    int toleranceMeters,
    boolean approvalRequired,
    List<StrictModeHours> strictModeHours
) {

    /**
     * Creates effective rules with a non-negative tolerance.
     */
    public ResolvedValidationRules {
        if (toleranceMeters < 0) {
            throw new IllegalArgumentException("Geofence tolerance cannot be negative");
        }
        strictModeHours = strictModeHours == null ? List.of() : List.copyOf(strictModeHours);
    }
}
