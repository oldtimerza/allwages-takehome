package com.allwage.clockin.controller.clock;

import com.allwage.clockin.model.ClockPage;
import com.allwage.clockin.model.ClockPageQuery;
import com.allwage.clockin.model.ValidatedClockEvent;
import com.allwage.clockin.service.ClockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for manager clock-attempt retrieval.
 */
@RestController
@Validated
public class ManagerClockController {

    private final ClockService clockService;

    public ManagerClockController(@NonNull ClockService clockService) {
        this.clockService = clockService;
    }

    /**
     * Retrieves an employee's clock attempts in newest-first order.
     *
     * @param employeeId employee identifier
     * @param request paging and optional status-filter parameters
     * @return clock attempts, or not found when the employee does not exist
     */
    @GetMapping("/api/employees/{employeeId}/clocks")
    public @NonNull ResponseEntity<ClockPageResponse> findForEmployee(
        @PathVariable String employeeId,
        @Valid @ModelAttribute ClockPageRequest request
    ) {
        return clockService.findPageForEmployee(employeeId, query(request))
            .map(ManagerClockController::response)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves a site's historically assigned clock attempts in newest-first order.
     *
     * @param siteId site identifier
     * @param request paging and optional status-filter parameters
     * @return clock attempts, or not found when the site does not exist
     */
    @GetMapping("/api/sites/{siteId}/clocks")
    public @NonNull ResponseEntity<ClockPageResponse> findForSite(
        @PathVariable String siteId,
        @Valid @ModelAttribute ClockPageRequest request
    ) {
        return clockService.findPageForSite(siteId, query(request))
            .map(ManagerClockController::response)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves a site team's historically assigned clock attempts in newest-first order.
     *
     * @param siteId site identifier
     * @param teamId site-local team identifier
     * @param request paging and optional status-filter parameters
     * @return clock attempts, or not found when the site or team does not exist
     */
    @GetMapping("/api/sites/{siteId}/teams/{teamId}/clocks")
    public @NonNull ResponseEntity<ClockPageResponse> findForTeam(
        @PathVariable String siteId,
        @PathVariable String teamId,
        @Valid @ModelAttribute ClockPageRequest request
    ) {
        return clockService.findPageForTeam(siteId, teamId, query(request))
            .map(ManagerClockController::response)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static ClockPageQuery query(ClockPageRequest request) {
        return new ClockPageQuery(request.page(), request.size(), request.status());
    }

    private static ClockPageResponse response(ClockPage page) {
        return new ClockPageResponse(
            page.entries().stream().map(ManagerClockController::clockResponse).toList(),
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages()
        );
    }

    private static ClockResponse clockResponse(ValidatedClockEvent clockEvent) {
        return new ClockResponse(clockEvent.clockEvent(), clockEvent.validationResult());
    }
}
