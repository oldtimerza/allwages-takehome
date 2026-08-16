package com.allwage.clockin.service.audit;

import java.util.Objects;

/**
 * Input, result, and request context visible to an audit mapper.
 */
public record AuditInvocation(
    AuditContext context,
    Object[] arguments,
    Object result
) {

    /**
     * Copies mutable method arguments before exposing them to an audit mapper.
     */
    public AuditInvocation {
        Objects.requireNonNull(context, "Audit context is required");
        arguments = Objects.requireNonNull(arguments, "Audit arguments are required").clone();
    }

    @Override
    public Object[] arguments() {
        return arguments.clone();
    }
}
