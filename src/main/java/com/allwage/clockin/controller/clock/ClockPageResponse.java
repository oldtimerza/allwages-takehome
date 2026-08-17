package com.allwage.clockin.controller.clock;

import java.util.List;

/**
 * HTTP response for a page of clock attempts.
 */
public record ClockPageResponse(
    List<ClockResponse> entries,
    int page,
    int size,
    int totalElements,
    int totalPages
) {
}
