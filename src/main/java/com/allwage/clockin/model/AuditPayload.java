package com.allwage.clockin.model;

/**
 * Type-specific data carried by an audit event.
 */
public sealed interface AuditPayload permits ClockAuditPayload {
}
