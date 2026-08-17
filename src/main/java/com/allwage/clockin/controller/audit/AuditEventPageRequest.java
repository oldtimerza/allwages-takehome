package com.allwage.clockin.controller.audit;

import com.allwage.clockin.model.audit.AuditEventType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * HTTP query parameters for one page of audit events.
 */
public record AuditEventPageRequest(
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size,
    AuditEventType type
) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;

    /**
     * Applies the public default page values when query parameters are absent.
     */
    public AuditEventPageRequest {
        page = page == null ? DEFAULT_PAGE : page;
        size = size == null ? DEFAULT_SIZE : size;
    }
}
