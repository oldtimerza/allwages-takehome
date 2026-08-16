package com.allwage.clockin.repository.audit;

import com.allwage.clockin.model.AuditEvent;
import com.allwage.clockin.model.AuditEventType;

import java.util.List;

/**
 * Persistence port for immutable audit event documents.
 */
public interface AuditEventRepository {

    /**
     * Appends an audit event without allowing an existing event ID to be replaced.
     *
     * @param auditEvent event to save
     */
    void save(AuditEvent auditEvent);

    /**
     * Finds audit events with a given stable type.
     *
     * @param type event type
     * @return matching audit events
     */
    List<AuditEvent> findByType(AuditEventType type);
}
