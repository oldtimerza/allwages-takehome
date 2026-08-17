package com.allwage.clockin.model;

/**
 * Domain query for one page of clock attempts.
 *
 * @param page zero-based page number
 * @param size maximum number of entries in the page
 * @param status optional validation decision filter
 */
public record ClockPageQuery(int page, int size, ClockValidationResult.Decision status) {

    /**
     * Creates a valid page query.
     */
    public ClockPageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Clock page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Clock page size must be positive");
        }
    }
}
