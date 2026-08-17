package com.allwage.clockin.model.audit;

/**
 * Type-specific data carried by an audit event.
 */
public sealed interface AuditPayload permits ClockAuditPayload {
}
