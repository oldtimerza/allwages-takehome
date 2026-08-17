package com.allwage.clockin.repository.clock;

import com.allwage.clockin.model.Site.ValidatedClockEvent;
import com.allwage.clockin.model.clock.ClockEvent;
import com.allwage.clockin.model.clock.ClockValidationResult;
import com.allwage.clockin.repository.store.DocumentStore;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClockEventDocumentStoreRepositoryTest {

    @Test
    void save_makesClockEventAvailableByIdAndInCollection() {
        // Given
        ClockEventDocumentStoreRepository repository = new ClockEventDocumentStoreRepository(new DocumentStore());
        ValidatedClockEvent clockEvent = clockEvent();

        // When
        repository.save(clockEvent);

        // Then
        assertThat(repository.findById(clockEvent.clockEvent().id())).contains(clockEvent);
        assertThat(repository.findAll()).containsExactly(clockEvent);
    }

    private ValidatedClockEvent clockEvent() {
        return new ValidatedClockEvent(
            new ClockEvent(
                "clock-1",
                "employee-1",
                ZonedDateTime.parse("2026-08-16T10:00:00+02:00"),
                -26.2041,
                28.0473,
                10,
                ClockEvent.ClockType.IN
            ),
            new ClockValidationResult(
                ClockValidationResult.Decision.ACCEPTED,
                ClockValidationResult.Reason.ACCEPTED,
                "site-1",
                "geofence-1"
            )
        );
    }
}
