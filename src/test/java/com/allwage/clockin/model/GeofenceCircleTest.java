package com.allwage.clockin.model;

import com.allwage.clockin.model.Site.GeoCoordinate;
import com.allwage.clockin.model.Site.GeofenceCircle;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GeofenceCircleTest {

    private static final GeofenceCircle GEOFENCE = new GeofenceCircle(
        "zone-1",
        new GeoCoordinate(-26.2041, 28.0473),
        100,
        true,
        LocalDate.of(2026, 1, 1),
        null
    );

    @Test
    void containsCoordinateWithinRadiusAndTolerance() {
        GeoCoordinate coordinateJustOutsideRadius = new GeoCoordinate(-26.20315, 28.0473);

        assertThat(GEOFENCE.contains(coordinateJustOutsideRadius, 10)).isTrue();
        assertThat(GEOFENCE.contains(coordinateJustOutsideRadius, 0)).isFalse();
    }
}
