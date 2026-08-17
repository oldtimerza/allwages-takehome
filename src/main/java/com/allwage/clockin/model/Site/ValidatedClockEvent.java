package com.allwage.clockin.model.Site;

import com.allwage.clockin.model.clock.ClockEvent;
import com.allwage.clockin.model.clock.ClockValidationResult;

import java.util.Objects;

/**
 * The stored clock attempt and the immutable validation decision made for it.
 */
public record ValidatedClockEvent(ClockEvent clockEvent, ClockValidationResult validationResult) {

    /**
     * Creates a stored clock record with its validation outcome.
     */
    public ValidatedClockEvent {
        Objects.requireNonNull(clockEvent, "Clock event is required");
        Objects.requireNonNull(validationResult, "Clock validation result is required");
    }
}
