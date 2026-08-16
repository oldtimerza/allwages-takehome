package com.allwage.clockin.repository.audit;

import com.allwage.clockin.model.AuditEvent;
import com.allwage.clockin.model.AuditEventType;
import com.allwage.clockin.repository.store.DocumentStore;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Document-store implementation of audit event persistence.
 */
@Repository
public class AuditEventDocumentStoreRepository implements AuditEventRepository {

    private static final String COLLECTION = "clock-audits";

    private final DocumentStore store;

    public AuditEventDocumentStoreRepository(@NonNull DocumentStore store) {
        this.store = store;
    }

    @Override
    public void save(@NonNull AuditEvent auditEvent) {
        store.saveIfAbsent(COLLECTION, auditEvent.id(), auditEvent);
    }

    @Override
    public @NonNull List<AuditEvent> findByType(@NonNull AuditEventType type) {
        return store.findAll(COLLECTION, AuditEvent.class).stream()
            .filter(auditEvent -> auditEvent.type() == type)
            .toList();
    }
}
