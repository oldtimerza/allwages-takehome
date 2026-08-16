package com.allwage.clockin.controller.site;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Creates a circular geofence owned by one site.
 */
public record CreateGeofenceRequest(
    @NotBlank(message = "Geofence ID is required")
    String id,

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    Double latitude,

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    Double longitude,

    @NotNull(message = "Geofence radius is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Geofence radius must be positive")
    Double radiusMeters,

    @NotNull(message = "Primary status is required")
    Boolean primary,

    @NotNull(message = "Geofence effective start date is required")
    LocalDate effectiveFrom,

    LocalDate effectiveTo
) {

    /**
     * Ensures a bounded geofence range does not end before it begins.
     *
     * @return true when the dates form a valid range
     */
    @AssertTrue(message = "Geofence effective end date cannot precede its start date")
    public boolean hasValidEffectiveRange() {
        return effectiveFrom == null || effectiveTo == null || !effectiveTo.isBefore(effectiveFrom);
    }
}
