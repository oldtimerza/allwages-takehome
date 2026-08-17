package com.allwage.clockin.repository.clock;

import com.allwage.clockin.model.Site.ValidatedClockEvent;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for validated clock event documents.
 */
public interface ClockEventRepository {

    /**
     * Saves a validated clock event.
     *
     * @param clockEvent validated clock event to save
     */
    void save(ValidatedClockEvent clockEvent);

    /**
     * Finds a validated clock event by identifier.
     *
     * @param id clock event identifier
     * @return clock event when present
     */
    Optional<ValidatedClockEvent> findById(String id);

    /**
     * Finds all validated clock events.
     *
     * @return stored clock events
     */
    List<ValidatedClockEvent> findAll();
}
