package com.allwage.clockin.model;

import java.util.List;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;

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

    /**
     * Resolves the tolerance that applies at a device clock timestamp in SAST.
     *
     * @param timestamp device clock timestamp
     * @return the base tolerance or the tightest active strict-mode tolerance
     */
    public int toleranceAt(ZonedDateTime timestamp) {
        ZonedDateTime sastTimestamp = timestamp.withZoneSameInstant(ZoneOffset.ofHours(2));
        return strictModeHours.stream()
            .filter(hours -> hours.days().contains(sastTimestamp.getDayOfWeek()))
            .filter(hours -> !sastTimestamp.toLocalTime().isBefore(hours.startsAt()))
            .filter(hours -> sastTimestamp.toLocalTime().isBefore(hours.endsAt()))
            .mapToInt(StrictModeHours::toleranceMeters)
            .reduce(toleranceMeters, Math::min);
    }
}
