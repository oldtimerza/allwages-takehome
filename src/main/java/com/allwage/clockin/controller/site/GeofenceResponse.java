package com.allwage.clockin.controller.site;

import java.time.LocalDate;

/**
 * Geofence details returned by geofence creation operations.
 */
public record GeofenceResponse(
    String id,
    double latitude,
    double longitude,
    double radiusMeters,
    boolean primary,
    LocalDate effectiveFrom,
    LocalDate effectiveTo
) { }
