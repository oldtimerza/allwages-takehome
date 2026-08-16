package com.allwage.clockin.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * An employee's historical or current membership of one team at a site.
 */
public record SiteAssignment(
    String employeeId,
    String teamId,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    ValidationRules validationRules
) {

    /**
     * Creates an assignment with an inclusive effective range.
     */
    public SiteAssignment {
        requireIdentifier(employeeId, "Employee ID");
        requireIdentifier(teamId, "Team ID");
        Objects.requireNonNull(effectiveFrom, "Assignment effective start date is required");
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Assignment effective end date cannot precede its start date");
        }
    }

    /**
     * Determines whether this assignment applies to a device clock date.
     *
     * @param date the clock date in SAST
     * @return true when the date is within the inclusive effective range
     */
    public boolean isEffectiveOn(LocalDate date) {
        Objects.requireNonNull(date, "Date is required");
        return !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    /**
     * Closes the assignment on the supplied inclusive date.
     *
     * @param endDate last date on which this assignment applies
     * @return the closed assignment
     */
    public SiteAssignment endOn(LocalDate endDate) {
        Objects.requireNonNull(endDate, "Assignment end date is required");
        if (endDate.isBefore(effectiveFrom)) {
            throw new IllegalArgumentException("Assignment end date cannot precede its start date");
        }
        return new SiteAssignment(employeeId, teamId, effectiveFrom, endDate, validationRules);
    }

    private static void requireIdentifier(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}
