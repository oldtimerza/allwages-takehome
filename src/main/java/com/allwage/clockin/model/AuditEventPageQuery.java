package com.allwage.clockin.model;

/**
 * Domain query for a page of audit events.
 *
 * @param page zero-based page number
 * @param size maximum number of entries in the page
 * @param type optional event-type filter
 */
public record AuditEventPageQuery(
    int page,
    int size,
    AuditEventType type
) {

    /**
     * Creates a valid page query.
     */
    public AuditEventPageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Audit page must not be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Audit page size must be positive");
        }
    }
}
