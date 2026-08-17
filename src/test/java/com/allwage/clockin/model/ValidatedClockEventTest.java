package com.allwage.clockin.model;

import com.allwage.clockin.model.Site.ValidatedClockEvent;
import com.allwage.clockin.model.clock.ClockEvent;
import com.allwage.clockin.model.clock.ClockValidationResult;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ValidatedClockEventTest {

    @Test
    void storesRawClockAttemptAlongsideItsValidationResult() {
        ClockEvent clockEvent = new ClockEvent(
            "clock-1",
            "employee-1",
            ZonedDateTime.parse("2026-01-05T07:00:00+02:00"),
            -26.2041,
            28.0473,
            10,
            ClockEvent.ClockType.IN
        );
        ClockValidationResult validationResult = new ClockValidationResult(
            ClockValidationResult.Decision.ACCEPTED,
            ClockValidationResult.Reason.ACCEPTED,
            "site-1",
            "zone-1"
        );

        ValidatedClockEvent validatedClockEvent = new ValidatedClockEvent(clockEvent, validationResult);

        assertThat(validatedClockEvent.clockEvent()).isEqualTo(clockEvent);
        assertThat(validatedClockEvent.validationResult()).isEqualTo(validationResult);
    }
}
