package com.allwage.clockin.service;

import com.allwage.clockin.model.AuditEventPage;
import com.allwage.clockin.model.AuditEventPageQuery;
import com.allwage.clockin.repository.audit.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Retrieves audit events for management investigation.
 */
@Service
public class AuditEventService {

    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(@NonNull AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Retrieves one ordered page of audit events.
     *
     * @param request page and optional type filter
     * @return matching event page
     */
    public @NonNull AuditEventPage findPage(@NonNull AuditEventPageQuery query) {
        AuditEventPage page = auditEventRepository.findPage(query);
        log.info("Retrieved audit event page {} with size {} and {} matching entries",
            query.page(), query.size(), page.totalElements());
        return page;
    }
}
