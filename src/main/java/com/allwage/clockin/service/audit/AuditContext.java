package com.allwage.clockin.service.audit;

/**
 * Safe request metadata that connects audit events and application logs.
 */
public record AuditContext(String correlationId) {

    /**
     * Creates a context with a usable correlation ID.
     */
    public AuditContext {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("Audit correlation ID is required");
        }
    }
}
