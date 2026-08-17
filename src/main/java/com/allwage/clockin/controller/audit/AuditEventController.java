package com.allwage.clockin.controller.audit;

import com.allwage.clockin.model.AuditEvent;
import com.allwage.clockin.model.AuditEventPage;
import com.allwage.clockin.model.AuditEventPageQuery;
import com.allwage.clockin.model.ClockAuditPayload;
import com.allwage.clockin.service.AuditEventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for read-only audit-event retrieval.
 */
@RestController
@Validated
@RequestMapping("/api/audit-events")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(@NonNull AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    /**
     * Retrieves audit events in newest-first order.
     *
     * @param request paging and event-type filter parameters
     * @return safe audit entries and pagination metadata
     */
    @GetMapping
    public @NonNull ResponseEntity<AuditEventPageResponse> findPage(
        @Valid @ModelAttribute AuditEventPageRequest request
    ) {
        AuditEventPage result = auditEventService.findPage(new AuditEventPageQuery(
            request.page(),
            request.size(),
            request.type()
        ));
        return ResponseEntity.ok(new AuditEventPageResponse(
            result.entries().stream().map(AuditEventController::response).toList(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages()
        ));
    }

    private static AuditEventResponse response(AuditEvent auditEvent) {
        ClockAuditPayload payload = (ClockAuditPayload) auditEvent.payload();
        return new AuditEventResponse(
            auditEvent.id(),
            auditEvent.occurredAt(),
            auditEvent.correlationId(),
            auditEvent.clockEventId(),
            auditEvent.employeeId(),
            auditEvent.type(),
            auditEvent.schemaVersion(),
            payload.reasonCode(),
            payload.source(),
            payload.httpStatus()
        );
    }
}
