package com.allwage.clockin.repository.audit;

import com.allwage.clockin.model.audit.AuditEvent;
import com.allwage.clockin.model.audit.AuditEventPage;
import com.allwage.clockin.model.audit.AuditEventPageQuery;
import com.allwage.clockin.model.audit.AuditEventType;
import com.allwage.clockin.repository.store.DocumentStore;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Comparator;

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

    @Override
    public @NonNull AuditEventPage findPage(@NonNull AuditEventPageQuery query) {
        List<AuditEvent> matchingEvents = store.findAll(COLLECTION, AuditEvent.class).stream()
            .filter(auditEvent -> query.type() == null || auditEvent.type() == query.type())
            .sorted(Comparator.comparing(AuditEvent::occurredAt).reversed()
                .thenComparing(AuditEvent::id, Comparator.reverseOrder()))
            .toList();
        int totalElements = matchingEvents.size();
        int totalPages = (totalElements + query.size() - 1) / query.size();
        long firstEntry = (long) query.page() * query.size();
        List<AuditEvent> entries = firstEntry >= totalElements
            ? List.of()
            : matchingEvents.subList((int) firstEntry, (int) Math.min(firstEntry + query.size(), totalElements));

        return new AuditEventPage(entries, query.page(), query.size(), totalElements, totalPages);
    }
}
