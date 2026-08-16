package com.allwage.clockin.controller.clock;

import com.allwage.clockin.model.ClockEvent;
import com.allwage.clockin.model.ValidatedClockEvent;
import com.allwage.clockin.service.ClockService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for clock-in/out operations.
 *
 * This provides a basic endpoint for receiving clock events.
 * You'll need to extend this with validation, error handling,
 * and additional endpoints as required.
 */
@RestController
@RequestMapping("/api/clocks")
public class ClockController {

    private final ClockService clockService;

    public ClockController(@NonNull ClockService clockService) {
        this.clockService = clockService;
    }

    /**
     * Process a clock-in or clock-out event from the mobile app.
     */
    @PostMapping
    public @NonNull ResponseEntity<ClockResponse> clock(@Valid @RequestBody @NonNull ClockRequest request) {
        ClockEvent event = new ClockEvent(
            UUID.randomUUID().toString(),
            request.employeeId(),
            request.timestamp(),
            request.latitude(),
            request.longitude(),
            request.accuracyMeters(),
            request.type()
        );

        ValidatedClockEvent saved = clockService.processClock(event);
        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Get all clock events.
     */
    @GetMapping
    public @NonNull ResponseEntity<List<ClockResponse>> getAll() {
        return ResponseEntity.ok(clockService.findAll().stream().map(this::toResponse).toList());
    }

    /**
     * Get a specific clock event by ID.
     */
    @GetMapping("/{id}")
    public @NonNull ResponseEntity<ClockResponse> getById(@PathVariable @NonNull String id) {
        return clockService.findById(id)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    private ClockResponse toResponse(ValidatedClockEvent clockEvent) {
        return new ClockResponse(clockEvent.clockEvent(), clockEvent.validationResult());
    }
}
