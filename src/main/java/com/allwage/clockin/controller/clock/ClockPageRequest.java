package com.allwage.clockin.controller.clock;

import com.allwage.clockin.model.ClockValidationResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * HTTP query parameters for one page of clock attempts.
 */
public record ClockPageRequest(
    @Min(0) Integer page,
    @Min(1) @Max(100) Integer size,
    ClockValidationResult.Decision status
) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;

    /**
     * Applies the public default page values when query parameters are absent.
     */
    public ClockPageRequest {
        page = page == null ? DEFAULT_PAGE : page;
        size = size == null ? DEFAULT_SIZE : size;
    }
}
