package com.allwage.clockin.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A circular zone that is valid for a bounded or open-ended date range.
 */
public record GeofenceCircle(
    String id,
    GeoCoordinate centre,
    double radiusMeters,
    boolean primary,
    LocalDate effectiveFrom,
    LocalDate effectiveTo
) {

    /**
     * Creates a geofence after validating its range and radius.
     */
    public GeofenceCircle {
        requireIdentifier(id, "Geofence ID");
        Objects.requireNonNull(centre, "Geofence centre is required");
        if (!Double.isFinite(radiusMeters) || radiusMeters <= 0) {
            throw new IllegalArgumentException("Geofence radius must be finite and positive");
        }
        Objects.requireNonNull(effectiveFrom, "Geofence effective start date is required");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Geofence effective end date cannot precede its start date");
        }
    }

    /**
     * Determines whether this geofence applies to a device clock date.
     *
     * @param date the clock date in SAST
     * @return true when the date is within the inclusive effective range
     */
    public boolean isEffectiveOn(LocalDate date) {
        Objects.requireNonNull(date, "Date is required");
        return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    private static void requireIdentifier(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}
