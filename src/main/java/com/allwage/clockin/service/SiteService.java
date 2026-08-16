package com.allwage.clockin.service;

import com.allwage.clockin.model.SiteAssignment;
import com.allwage.clockin.model.Site;
import com.allwage.clockin.model.GeofenceCircle;
import com.allwage.clockin.model.Team;
import com.allwage.clockin.model.ValidationRules;
import com.allwage.clockin.repository.employee.EmployeeRepository;
import com.allwage.clockin.repository.site.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Coordinates updates to site-owned configuration and assignments.
 */
@Service
public class SiteService {

    private static final Logger log = LoggerFactory.getLogger(SiteService.class);

    private final SiteRepository siteRepository;
    private final EmployeeRepository employeeRepository;

    public SiteService(@NonNull SiteRepository siteRepository, @NonNull EmployeeRepository employeeRepository) {
        this.siteRepository = siteRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Creates a site when its identifier is not already in use.
     *
     * @param site site to create
     * @return created site, or empty when the identifier already exists
     */
    public @NonNull Optional<Site> createSite(@NonNull Site site) {
        if (!siteRepository.saveIfAbsent(site)) {
            log.warn("Cannot create site because siteId={} already exists", site.id());
            return Optional.empty();
        }
        log.info("Created site: siteId={}", site.id());
        return Optional.of(site);
    }

    /**
     * Adds a site-owned team.
     *
     * @param siteId site identifier
     * @param team team to add
     * @return added team when the site exists
     */
    public @NonNull Optional<Team> addTeam(@NonNull String siteId, @NonNull Team team) {
        Optional<Team> addedTeam = siteRepository.update(siteId, site -> Optional.of(site.addTeam(team)))
            .map(updatedSite -> logTeamAdded(updatedSite, team));
        if (addedTeam.isEmpty()) {
            log.warn("Cannot add team because siteId={} does not exist", siteId);
        }
        return addedTeam;
    }

    /**
     * Adds a site-owned geofence.
     *
     * @param siteId site identifier
     * @param geofence geofence to add
     * @return added geofence when the site exists
     */
    public @NonNull Optional<GeofenceCircle> addGeofence(@NonNull String siteId, @NonNull GeofenceCircle geofence) {
        Optional<GeofenceCircle> addedGeofence = siteRepository.update(
            siteId,
            site -> Optional.of(site.addGeofence(geofence))
        )
            .map(updatedSite -> logGeofenceAdded(updatedSite, geofence));
        if (addedGeofence.isEmpty()) {
            log.warn("Cannot add geofence because siteId={} does not exist", siteId);
        }
        return addedGeofence;
    }

    /**
     * Assigns an existing employee to a site-owned team.
     *
     * @param siteId site identifier
     * @param teamId site-local team identifier
     * @param employeeId employee identifier
     * @param effectiveFrom first date of the assignment
     * @param validationRules optional employee-specific rule overrides
     * @return assignment when the employee, site, and team exist
     */
    public @NonNull Optional<SiteAssignment> assignEmployee(
        @NonNull String siteId,
        @NonNull String teamId,
        @NonNull String employeeId,
        @NonNull LocalDate effectiveFrom,
        ValidationRules validationRules
    ) {
        Optional<SiteAssignment> assignment = siteRepository.update(
            siteId,
            site -> assignToTeam(site, teamId, employeeId, effectiveFrom, validationRules)
        )
            .map(updatedSite -> assignmentAdded(updatedSite, employeeId, effectiveFrom));
        if (assignment.isEmpty()) {
            log.warn(
                "No employee assignment was created: siteId={} teamId={} employeeId={} effectiveFrom={}",
                siteId, teamId, employeeId, effectiveFrom
            );
        }
        return assignment;
    }

    /**
     * Replaces a site's default validation-rule overrides.
     *
     * @param siteId site identifier
     * @param rules replacement rule overrides
     * @return replacement rules when the site exists
     */
    public @NonNull Optional<ValidationRules> replaceSiteValidationRules(
        @NonNull String siteId,
        @NonNull ValidationRules rules
    ) {
        return siteRepository.update(siteId, site -> Optional.of(site.withValidationRules(rules)))
            .map(updatedSite -> logSiteRulesUpdated(updatedSite, rules))
            .or(() -> validationRulesNotFound(siteId, "site"));
    }

    /**
     * Replaces validation-rule overrides for a team at a site.
     *
     * @param siteId site identifier
     * @param teamId site-local team identifier
     * @param rules replacement rule overrides
     * @return replacement rules when the site and team exist
     */
    public @NonNull Optional<ValidationRules> replaceTeamValidationRules(
        @NonNull String siteId,
        @NonNull String teamId,
        @NonNull ValidationRules rules
    ) {
        return siteRepository.update(siteId, site -> site.withTeamValidationRules(teamId, rules))
            .map(updatedSite -> logTeamRulesUpdated(updatedSite, teamId, rules))
            .or(() -> validationRulesNotFound(siteId, "site or team"));
    }

    /**
     * Replaces validation-rule overrides for an employee's dated site assignment.
     *
     * @param siteId site identifier
     * @param employeeId employee identifier
     * @param effectiveFrom first date of the assignment
     * @param rules replacement rule overrides
     * @return replacement rules when the site and assignment exist
     */
    public @NonNull Optional<ValidationRules> replaceAssignmentValidationRules(
        @NonNull String siteId,
        @NonNull String employeeId,
        @NonNull LocalDate effectiveFrom,
        @NonNull ValidationRules rules
    ) {
        return siteRepository.update(
            siteId,
            site -> site.withAssignmentValidationRules(employeeId, effectiveFrom, rules)
        )
            .map(updatedSite -> logAssignmentRulesUpdated(updatedSite, employeeId, effectiveFrom, rules))
            .or(() -> validationRulesNotFound(siteId, "site or employee assignment"));
    }

    private ValidationRules logSiteRulesUpdated(Site site, ValidationRules rules) {
        log.info("Replaced site validation rules for siteId={}", site.id());
        return rules;
    }

    private Team logTeamAdded(Site site, Team team) {
        log.info("Added team to site: siteId={} teamId={}", site.id(), team.id());
        return team;
    }

    private GeofenceCircle logGeofenceAdded(Site site, GeofenceCircle geofence) {
        log.info("Added geofence to site: siteId={} geofenceId={}", site.id(), geofence.id());
        return geofence;
    }

    private Optional<Site> assignToTeam(
        Site site,
        String teamId,
        String employeeId,
        LocalDate effectiveFrom,
        ValidationRules validationRules
    ) {
        if (employeeRepository.findById(employeeId).isEmpty()) {
            log.warn("Cannot assign employee because employeeId={} does not exist", employeeId);
            return Optional.empty();
        }
        if (site.teams().stream().noneMatch(team -> team.id().equals(teamId))) {
            log.warn("Cannot assign employee because teamId={} does not belong to siteId={}", teamId, site.id());
            return Optional.empty();
        }
        return Optional.of(site.assignEmployee(employeeId, teamId, effectiveFrom, validationRules));
    }

    private SiteAssignment assignmentAdded(Site site, String employeeId, LocalDate effectiveFrom) {
        SiteAssignment assignment = site.assignments().stream()
            .filter(candidate -> candidate.employeeId().equals(employeeId)
                && candidate.effectiveFrom().equals(effectiveFrom))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Saved site is missing its new employee assignment"));
        log.info(
            "Assigned employee to site team: siteId={} teamId={} employeeId={} effectiveFrom={}",
            site.id(), assignment.teamId(), employeeId, effectiveFrom
        );
        return assignment;
    }

    private ValidationRules logTeamRulesUpdated(Site site, String teamId, ValidationRules rules) {
        log.info("Replaced team validation rules for siteId={} teamId={}", site.id(), teamId);
        return rules;
    }

    private ValidationRules logAssignmentRulesUpdated(
        Site site,
        String employeeId,
        LocalDate effectiveFrom,
        ValidationRules rules
    ) {
        log.info(
            "Replaced employee validation rules for siteId={} employeeId={} effectiveFrom={}",
            site.id(), employeeId, effectiveFrom
        );
        return rules;
    }

    private Optional<ValidationRules> validationRulesNotFound(String siteId, String scope) {
        log.warn("Cannot replace validation rules because {} does not exist for siteId={}", scope, siteId);
        return Optional.empty();
    }
}
