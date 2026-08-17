package com.allwage.clockin.model;

import java.util.List;
import java.util.Objects;

/**
 * One immutable result page from an audit-event query.
 *
 * @param entries audit entries in the requested order
 * @param page zero-based page number
 * @param size requested maximum page size
 * @param totalElements number of matching events
 * @param totalPages number of available pages
 */
public record AuditEventPage(
    List<AuditEvent> entries,
    int page,
    int size,
    int totalElements,
    int totalPages
) {

    /**
     * Creates a page with internally immutable entries and valid metadata.
     */
    public AuditEventPage {
        entries = List.copyOf(Objects.requireNonNull(entries, "Audit entries are required"));
        if (page < 0 || size < 1 || totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException("Audit page metadata must be valid");
        }
    }
}
