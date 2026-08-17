package com.allwage.clockin.service.audit;

import com.allwage.clockin.model.audit.AuditEventType;
import com.allwage.clockin.model.audit.AuditPayload;

import java.util.Objects;

/**
 * Audit event data produced by a mapper before the writer adds envelope metadata.
 */
public record AuditDraft(
    String clockEventId,
    String employeeId,
    AuditEventType type,
    AuditPayload payload
) {

    /**
     * Creates an audit draft with required event details.
     */
    public AuditDraft {
        Objects.requireNonNull(type, "Audit event type is required");
        Objects.requireNonNull(payload, "Audit payload is required");
    }
}
