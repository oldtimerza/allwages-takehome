package com.allwage.clockin.controller.clock;

import com.allwage.clockin.model.clock.ClockEvent;
import com.allwage.clockin.model.clock.ClockValidationResult;

/**
 * HTTP representation of a stored clock attempt and its validation decision.
 */
public record ClockResponse(ClockEvent clockEvent, ClockValidationResult validationResult) { }
