package com.allwage.clockin.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable, versioned record of a clock processing outcome.
 */
public record AuditEvent(
    String id,
    Instant occurredAt,
    String correlationId,
    String clockEventId,
    String employeeId,
    AuditEventType type,
    int schemaVersion,
    AuditPayload payload
) {

    /**
     * Creates an audit event with required envelope fields.
     */
    public AuditEvent {
        requireText(id, "Audit event ID");
        Objects.requireNonNull(occurredAt, "Audit occurrence time is required");
        requireText(correlationId, "Audit correlation ID");
        Objects.requireNonNull(type, "Audit event type is required");
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Audit schema version must be positive");
        }
        Objects.requireNonNull(payload, "Audit payload is required");
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}
