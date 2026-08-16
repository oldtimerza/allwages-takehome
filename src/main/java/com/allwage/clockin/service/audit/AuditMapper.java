package com.allwage.clockin.service.audit;

import java.util.Optional;

/**
 * Converts the inputs and outcomes of one use case into audit event data.
 */
public interface AuditMapper {

    /**
     * Builds an audit event after the audited operation succeeds.
     *
     * @param invocation audited operation details
     * @return audit event data
     */
    AuditDraft onSuccess(AuditInvocation invocation);

    /**
     * Optionally builds an audit event after the audited operation fails.
     *
     * @param invocation audited operation details
     * @param failure operation failure
     * @return audit event data when the failure is auditable
     */
    default Optional<AuditDraft> onFailure(AuditInvocation invocation, Throwable failure) {
        return Optional.empty();
    }
}
