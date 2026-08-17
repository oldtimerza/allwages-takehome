package com.allwage.clockin.controller.audit;

import com.allwage.clockin.model.audit.AuditEventType;
import com.allwage.clockin.model.audit.AuditReasonCode;
import com.allwage.clockin.model.audit.AuditSource;

import java.time.Instant;

/**
 * Safe public representation of one audit event.
 */
public record AuditEventResponse(
    String id,
    Instant occurredAt,
    String correlationId,
    String clockEventId,
    String employeeId,
    AuditEventType type,
    int schemaVersion,
    AuditReasonCode reasonCode,
    AuditSource source,
    int httpStatus
) {
}
