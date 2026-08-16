package com.allwage.clockin.service;

import com.allwage.clockin.client.InstantMessagingClient;
import com.allwage.clockin.model.ClockEvent;
import com.allwage.clockin.model.ClockValidationResult;
import com.allwage.clockin.model.Employee;
import com.allwage.clockin.model.GeoCoordinate;
import com.allwage.clockin.model.GeofenceCircle;
import com.allwage.clockin.model.ResolvedValidationRules;
import com.allwage.clockin.model.Site;
import com.allwage.clockin.model.ValidatedClockEvent;
import com.allwage.clockin.repository.employee.EmployeeRepository;
import com.allwage.clockin.repository.site.SiteRepository;
import com.allwage.clockin.repository.store.DocumentStore;
import com.allwage.clockin.service.audit.AuditWriter;
import com.allwage.clockin.service.audit.Audited;
import com.allwage.clockin.service.audit.ClockProcessingAuditMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling clock events.
 *
 * Validates incoming clock events against an employee's assigned site geofences.
 */
@Service
public class ClockService {

    private static final Logger log = LoggerFactory.getLogger(ClockService.class);

    private final DocumentStore store;
    private final SiteRepository siteRepository;
    private final EmployeeRepository employeeRepository;
    private final InstantMessagingClient instantMessagingClient;
    private final AuditWriter auditWriter;
    private final ClockProcessingAuditMapper auditMapper;

    public ClockService(
        @NonNull DocumentStore store,
        @NonNull SiteRepository siteRepository,
        @NonNull EmployeeRepository employeeRepository,
        @NonNull InstantMessagingClient instantMessagingClient,
        @NonNull AuditWriter auditWriter,
        @NonNull ClockProcessingAuditMapper auditMapper
    ) {
        this.store = store;
        this.siteRepository = siteRepository;
        this.employeeRepository = employeeRepository;
        this.instantMessagingClient = instantMessagingClient;
        this.auditWriter = auditWriter;
        this.auditMapper = auditMapper;
    }

    /**
     * Process an incoming clock event from the mobile app.
     *
     * @param clockEvent The clock event to process
     * @return the persisted event with its validation outcome
     */
    @Audited(mapper = ClockProcessingAuditMapper.class, auditSuccess = false)
    public @NonNull ValidatedClockEvent processClock(@NonNull ClockEvent clockEvent) {
        Employee employee = employeeRepository.findById(clockEvent.employeeId()).orElse(null);
        ValidatedClockEvent processedClock = new ValidatedClockEvent(clockEvent, validate(clockEvent, employee != null));
        ValidatedClockEvent persistedClock = persist(processedClock);
        log.info("Processed clock {} for employee {} with decision {} and reason {}",
            persistedClock.clockEvent().id(), persistedClock.clockEvent().employeeId(), persistedClock.validationResult().decision(),
            persistedClock.validationResult().reason());
        if (employee != null) {
            notifyEmployee(employee, persistedClock);
        }
        return persistedClock;
    }

    /**
     * Find a clock event by ID.
     */
    public @NonNull Optional<ValidatedClockEvent> findById(@NonNull String id) {
        return store.findById("clocks", id, ValidatedClockEvent.class);
    }

    /**
     * Get all clock events.
     */
    public @NonNull List<ValidatedClockEvent> findAll() {
        return store.findAll("clocks", ValidatedClockEvent.class);
    }

    private ClockValidationResult validate(ClockEvent clockEvent, boolean employeeExists) {
        if (!employeeExists) {
            return rejected(ClockValidationResult.Reason.EMPLOYEE_NOT_FOUND, null, null);
        }
        LocalDate clockDate = clockDate(clockEvent);
        List<Site> assignedSites = assignedSitesFor(clockEvent, clockDate);
        if (assignedSites.isEmpty()) {
            return rejected(ClockValidationResult.Reason.NO_SITE_ASSIGNMENT, null, null);
        }
        List<SiteMatch> matchingGeofences = matchingGeofencesFor(clockEvent, clockDate, assignedSites);
        if (matchingGeofences.isEmpty()) {
            return rejected(ClockValidationResult.Reason.OUTSIDE_GEOFENCE, null, null);
        }
        if (matchesMultipleSites(matchingGeofences)) {
            return rejected(ClockValidationResult.Reason.AMBIGUOUS_SITE, null, null);
        }
        return validationFor(selectPreferredGeofence(matchingGeofences));
    }

    private LocalDate clockDate(ClockEvent clockEvent) {
        return clockEvent.timestamp().withZoneSameInstant(ZoneOffset.ofHours(2)).toLocalDate();
    }

    private List<Site> assignedSitesFor(ClockEvent clockEvent, LocalDate clockDate) {
        return siteRepository.findAssignedTo(clockEvent.employeeId(), clockDate);
    }

    private List<SiteMatch> matchingGeofencesFor(
        ClockEvent clockEvent,
        LocalDate clockDate,
        List<Site> assignedSites
    ) {
        GeoCoordinate coordinate = new GeoCoordinate(clockEvent.latitude(), clockEvent.longitude());
        return assignedSites.stream()
            .flatMap(site -> matchingGeofences(site, clockEvent, clockDate, coordinate).stream())
            .toList();
    }

    private boolean matchesMultipleSites(List<SiteMatch> matchingGeofences) {
        return matchingGeofences.stream().map(match -> match.site().id()).distinct().count() > 1;
    }

    private SiteMatch selectPreferredGeofence(List<SiteMatch> matchingGeofences) {
        return matchingGeofences.stream()
            .min(Comparator.comparing((SiteMatch match) -> !match.geofence().primary())
                .thenComparingDouble(SiteMatch::distanceMeters))
            .orElseThrow();
    }

    private ClockValidationResult validationFor(SiteMatch selectedMatch) {
        if (selectedMatch.rules().approvalRequired() && !selectedMatch.geofence().primary()) {
            return rejected(ClockValidationResult.Reason.APPROVAL_REQUIRED, selectedMatch.site().id(),
                selectedMatch.geofence().id());
        }
        return new ClockValidationResult(ClockValidationResult.Decision.ACCEPTED,
            ClockValidationResult.Reason.ACCEPTED, selectedMatch.site().id(), selectedMatch.geofence().id());
    }

    private ValidatedClockEvent persist(ValidatedClockEvent clockEvent) {
        return store.executeAtomically(() -> {
            store.save("clocks", clockEvent.clockEvent().id(), clockEvent);
            auditWriter.append(auditMapper.auditFor(clockEvent));
            return clockEvent;
        });
    }

    private List<SiteMatch> matchingGeofences(
        Site site,
        ClockEvent clockEvent,
        LocalDate clockDate,
        GeoCoordinate coordinate
    ) {
        ResolvedValidationRules rules = site.rulesFor(clockEvent.employeeId(), clockDate);
        int toleranceMeters = rules.toleranceAt(clockEvent.timestamp());
        return site.geofences().stream()
            .filter(geofence -> geofence.isEffectiveOn(clockDate))
            .filter(geofence -> geofence.contains(coordinate, toleranceMeters))
            .map(geofence -> new SiteMatch(site, geofence, rules, geofence.distanceToMeters(coordinate)))
            .toList();
    }

    private ClockValidationResult rejected(ClockValidationResult.Reason reason, String siteId, String geofenceId) {
        return new ClockValidationResult(ClockValidationResult.Decision.REJECTED, reason, siteId, geofenceId);
    }

    private void notifyEmployee(Employee employee, ValidatedClockEvent clockEvent) {
        String message = clockEvent.validationResult().decision() == ClockValidationResult.Decision.ACCEPTED
            ? "Your " + clockType(clockEvent) + " was accepted."
            : "Your " + clockType(clockEvent) + " requires attention.";
        try {
            if (!instantMessagingClient.sendMessage(employee.phoneNumber(), message)) {
                log.warn("Clock notification was not accepted for employee {} and clock {}",
                    employee.id(), clockEvent.clockEvent().id());
            }
        } catch (RuntimeException exception) {
            log.warn("Clock notification failed for employee {} and clock {}", employee.id(), clockEvent.clockEvent().id(), exception);
        }
    }

    private String clockType(ValidatedClockEvent clockEvent) {
        return clockEvent.clockEvent().type() == ClockEvent.ClockType.IN ? "clock-in" : "clock-out";
    }

    private record SiteMatch(Site site, GeofenceCircle geofence, ResolvedValidationRules rules, double distanceMeters) { }
}
