package com.allwage.clockin.controller.clock;

import com.allwage.clockin.model.clock.ClockEvent.ClockType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.time.ZonedDateTime;

/**
 * Request body for clock-in/out endpoint.
 *
 * Timestamps should be provided in ISO-8601 format.
 * All times are assumed to be in SAST (South African Standard Time, UTC+2).
 */
public record ClockRequest(
    @NotBlank(message = "Employee ID is required")
    String employeeId,

    @NotNull(message = "Timestamp is required")
    ZonedDateTime timestamp,

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
    @DecimalMax(value = "90.0", message = "Latitude must be at most 90")
    Double latitude,

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
    @DecimalMax(value = "180.0", message = "Longitude must be at most 180")
    Double longitude,

    @NotNull(message = "Accuracy is required")
    @DecimalMin(value = "0.0", message = "Accuracy cannot be negative")
    Double accuracyMeters,

    @NotNull(message = "Clock type is required")
    ClockType type
) { }
