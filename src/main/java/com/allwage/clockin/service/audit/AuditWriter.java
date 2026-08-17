package com.allwage.clockin.service.audit;

import com.allwage.clockin.model.audit.AuditEvent;
import com.allwage.clockin.repository.audit.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Assigns immutable audit envelopes and persists them through the audit repository.
 */
@Service
public class AuditWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditWriter.class);
    private static final int SCHEMA_VERSION = 1;

    private final AuditEventRepository auditEventRepository;
    private final AuditContextProvider auditContextProvider;

    public AuditWriter(
        @NonNull AuditEventRepository auditEventRepository,
        @NonNull AuditContextProvider auditContextProvider
    ) {
        this.auditEventRepository = auditEventRepository;
        this.auditContextProvider = auditContextProvider;
    }

    /**
     * Stores an immutable audit event for the current request context.
     *
     * @param draft mapper-produced audit data
     */
    public void append(@NonNull AuditDraft draft) {
        AuditEvent auditEvent = new AuditEvent(
            UUID.randomUUID().toString(),
            Instant.now(),
            auditContextProvider.current().correlationId(),
            draft.clockEventId(),
            draft.employeeId(),
            draft.type(),
            SCHEMA_VERSION,
            draft.payload()
        );
        auditEventRepository.save(auditEvent);
        log.info("Recorded audit event type {} for clock {} with correlation {}",
            auditEvent.type(), auditEvent.clockEventId(), auditEvent.correlationId());
    }
}
