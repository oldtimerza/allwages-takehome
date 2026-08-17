package com.allwage.clockin.repository.audit;

import com.allwage.clockin.model.audit.AuditEvent;
import com.allwage.clockin.model.audit.AuditEventPage;
import com.allwage.clockin.model.audit.AuditEventPageQuery;
import com.allwage.clockin.model.audit.AuditEventType;

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

    /**
     * Finds a deterministic page of audit events.
     *
     * @param query requested page and optional type filter
     * @return matching audit events and page metadata
     */
    AuditEventPage findPage(AuditEventPageQuery query);
}
