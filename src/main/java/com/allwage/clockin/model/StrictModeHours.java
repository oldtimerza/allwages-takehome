package com.allwage.clockin.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;

/**
 * A weekly time window with a tighter geofence tolerance.
 */
public record StrictModeHours(
    Set<DayOfWeek> days,
    LocalTime startsAt,
    LocalTime endsAt,
    int toleranceMeters
) {

    /**
     * Creates a non-overnight strict-mode window.
     */
    public StrictModeHours {
        days = Set.copyOf(Objects.requireNonNull(days, "Strict-mode days are required"));
        if (days.isEmpty()) {
            throw new IllegalArgumentException("Strict-mode days are required");
        }
        Objects.requireNonNull(startsAt, "Strict-mode start time is required");
        Objects.requireNonNull(endsAt, "Strict-mode end time is required");
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Strict-mode end time must follow its start time");
        }
        if (toleranceMeters < 0) {
            throw new IllegalArgumentException("Strict-mode tolerance cannot be negative");
        }
    }
}
