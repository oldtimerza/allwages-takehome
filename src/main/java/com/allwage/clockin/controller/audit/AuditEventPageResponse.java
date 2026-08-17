package com.allwage.clockin.controller.audit;

import java.util.List;

/**
 * HTTP response for a page of audit events.
 */
public record AuditEventPageResponse(
    List<AuditEventResponse> entries,
    int page,
    int size,
    int totalElements,
    int totalPages
) {
}
