package com.allwage.clockin.model;

import java.util.List;
import java.util.Objects;

/**
 * One immutable result page from a clock-attempt query.
 *
 * @param entries clock attempts in the requested order
 * @param page zero-based page number
 * @param size requested maximum page size
 * @param totalElements number of matching attempts
 * @param totalPages number of available pages
 */
public record ClockPage(
    List<ValidatedClockEvent> entries,
    int page,
    int size,
    int totalElements,
    int totalPages
) {

    /**
     * Creates a page with internally immutable entries and valid metadata.
     */
    public ClockPage {
        entries = List.copyOf(Objects.requireNonNull(entries, "Clock entries are required"));
        if (page < 0 || size < 1 || totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException("Clock page metadata must be valid");
        }
    }
}
