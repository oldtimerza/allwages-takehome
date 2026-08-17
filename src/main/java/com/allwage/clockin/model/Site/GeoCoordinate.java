package com.allwage.clockin.model.Site;

/**
 * A geographic coordinate in decimal degrees.
 */
public record GeoCoordinate(double latitude, double longitude) {

    /**
     * Creates a coordinate after validating the valid geographic range.
     */
    public GeoCoordinate {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be finite and between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be finite and between -180 and 180");
        }
    }
}
