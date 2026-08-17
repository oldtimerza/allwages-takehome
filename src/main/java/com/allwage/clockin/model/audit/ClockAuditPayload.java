package com.allwage.clockin.model.audit;

import java.util.Objects;

/**
 * Type-specific outcome details for a clock submission audit event.
 */
public record ClockAuditPayload(
    AuditReasonCode reasonCode,
    AuditSource source,
    int httpStatus
) implements AuditPayload {

    /**
     * Creates clock audit details with a valid HTTP status.
     */
    public ClockAuditPayload {
        Objects.requireNonNull(reasonCode, "Audit reason code is required");
        Objects.requireNonNull(source, "Audit source is required");
        if (httpStatus < 100 || httpStatus > 599) {
            throw new IllegalArgumentException("Audit HTTP status must be valid");
        }
    }
}
