package com.allwage.clockin.model.Site;

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

    /**
     * Determines whether a coordinate lies within this zone including the configured tolerance.
     *
     * @param coordinate device coordinate
     * @param toleranceMeters additional permitted distance beyond the radius
     * @return true when the coordinate is contained by the effective radius
     */
    public boolean contains(GeoCoordinate coordinate, int toleranceMeters) {
        if (toleranceMeters < 0) {
            throw new IllegalArgumentException("Geofence tolerance cannot be negative");
        }
        return distanceToMeters(coordinate) <= radiusMeters + toleranceMeters;
    }

    /**
     * Calculates the great-circle distance from this zone's centre to a coordinate.
     *
     * @param coordinate coordinate to measure
     * @return distance in metres
     */
    public double distanceToMeters(GeoCoordinate coordinate) {
        Objects.requireNonNull(coordinate, "Coordinate is required");
        double latitudeDifference = Math.toRadians(coordinate.latitude() - centre.latitude());
        double longitudeDifference = Math.toRadians(coordinate.longitude() - centre.longitude());
        double latitudeStart = Math.toRadians(centre.latitude());
        double latitudeEnd = Math.toRadians(coordinate.latitude());
        double haversine = Math.sin(latitudeDifference / 2) * Math.sin(latitudeDifference / 2)
            + Math.cos(latitudeStart) * Math.cos(latitudeEnd)
            * Math.sin(longitudeDifference / 2) * Math.sin(longitudeDifference / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
    }

    private static void requireIdentifier(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}
