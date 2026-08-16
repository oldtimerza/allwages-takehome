package com.allwage.clockin.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The document aggregate that owns teams, geofences, and employee assignments.
 */
public record Site(
    String id,
    String name,
    ValidationRules validationRules,
    List<Team> teams,
    List<GeofenceCircle> geofences,
    List<SiteAssignment> assignments
) {

    private static final int DEFAULT_TOLERANCE_METERS = 20;

    /**
     * Creates a site document while preserving ownership and uniqueness invariants.
     */
    public Site {
        requireIdentifier(id, "Site ID");
        requireIdentifier(name, "Site name");
        teams = List.copyOf(Objects.requireNonNull(teams, "Site teams are required"));
        geofences = List.copyOf(Objects.requireNonNull(geofences, "Site geofences are required"));
        assignments = List.copyOf(Objects.requireNonNull(assignments, "Site assignments are required"));
        requireUniqueTeamIds(teams);
        requireUniqueGeofenceIds(geofences);
        requireValidAssignments(teams, assignments);
        requireNonOverlappingAssignments(id, assignments);
        requireNonOverlappingPrimaryGeofences(geofences);
    }

    /**
     * Adds a team that can receive employee assignments at this site.
     *
     * @param team the team to add
     * @return an updated site document
     */
    public Site addTeam(Team team) {
        Objects.requireNonNull(team, "Team is required");
        if (teams.stream().anyMatch(existingTeam -> existingTeam.id().equals(team.id()))) {
            throw new IllegalArgumentException("Team " + team.id() + " already belongs to site " + id);
        }
        List<Team> updatedTeams = new ArrayList<>(teams);
        updatedTeams.add(team);
        return new Site(id, name, validationRules, updatedTeams, geofences, assignments);
    }

    /**
     * Adds a site-owned geofence.
     *
     * @param geofence the geofence to add
     * @return an updated site document
     */
    public Site addGeofence(GeofenceCircle geofence) {
        Objects.requireNonNull(geofence, "Geofence is required");
        if (geofences.stream().anyMatch(existingGeofence -> existingGeofence.id().equals(geofence.id()))) {
            throw new IllegalArgumentException("Geofence " + geofence.id() + " already belongs to site " + id);
        }
        List<GeofenceCircle> updatedGeofences = new ArrayList<>(geofences);
        updatedGeofences.add(geofence);
        return new Site(id, name, validationRules, teams, updatedGeofences, assignments);
    }

    /**
     * Assigns an employee to one team at this site, closing their current assignment when replaced.
     *
     * @param employeeId employee identifier
     * @param teamId site-local team identifier
     * @param effectiveFrom first date in the new assignment
     * @param employeeRules optional employee override for this site assignment
     * @return an updated site document
     */
    public Site assignEmployee(String employeeId, String teamId, LocalDate effectiveFrom, ValidationRules employeeRules) {
        Objects.requireNonNull(effectiveFrom, "Assignment effective start date is required");
        if (teams.stream().noneMatch(team -> team.id().equals(teamId))) {
            throw new IllegalArgumentException("Team " + teamId + " does not belong to site " + id);
        }
        if (assignments.stream().anyMatch(assignment -> assignment.employeeId().equals(employeeId)
            && assignment.effectiveFrom().isAfter(effectiveFrom))) {
            throw new IllegalArgumentException("Employee " + employeeId + " has a later assignment at site " + id);
        }
        List<SiteAssignment> updatedAssignments = new ArrayList<>();
        for (SiteAssignment assignment : assignments) {
            SiteAssignment updatedAssignment = closeIfReplaced(assignment, employeeId, effectiveFrom);
            if (updatedAssignment != null) {
                updatedAssignments.add(updatedAssignment);
            }
        }
        updatedAssignments.add(new SiteAssignment(employeeId, teamId, effectiveFrom, null, employeeRules));
        return new Site(id, name, validationRules, teams, geofences, updatedAssignments);
    }

    /**
     * Finds an employee's assignment that was active when their device clocked.
     *
     * @param employeeId employee identifier
     * @param date device clock date in SAST
     * @return the active assignment, if any
     */
    public Optional<SiteAssignment> assignmentFor(String employeeId, LocalDate date) {
        return assignments.stream()
            .filter(assignment -> assignment.employeeId().equals(employeeId) && assignment.isEffectiveOn(date))
            .findFirst();
    }

    /**
     * Resolves validation rules for an employee assignment using specific-over-general precedence.
     *
     * @param employeeId employee identifier
     * @param date device clock date in SAST
     * @return the resolved validation rules
     */
    public ResolvedValidationRules rulesFor(String employeeId, LocalDate date) {
        SiteAssignment assignment = assignmentFor(employeeId, date)
            .orElseThrow(() -> new IllegalArgumentException("Employee " + employeeId + " is not assigned to site " + id));
        Team team = teams.stream()
            .filter(candidate -> candidate.id().equals(assignment.teamId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Assignment references an unknown site team"));
        return new ResolvedValidationRules(
            firstDefined(assignment.validationRules(), team.validationRules(), validationRules, DEFAULT_TOLERANCE_METERS),
            firstDefined(assignment.validationRules(), team.validationRules(), validationRules, false),
            firstDefined(assignment.validationRules(), team.validationRules(), validationRules)
        );
    }

    private static SiteAssignment closeIfReplaced(
        SiteAssignment assignment,
        String employeeId,
        LocalDate newAssignmentStart
    ) {
        if (assignment.employeeId().equals(employeeId) && assignment.isEffectiveOn(newAssignmentStart)) {
            if (assignment.effectiveFrom().equals(newAssignmentStart)) {
                return null;
            }
            return assignment.endOn(newAssignmentStart.minusDays(1));
        }
        return assignment;
    }

    private static int firstDefined(
        ValidationRules employeeRules,
        ValidationRules teamRules,
        ValidationRules siteRules,
        int defaultValue
    ) {
        if (employeeRules != null && employeeRules.toleranceMeters() != null) {
            return employeeRules.toleranceMeters();
        }
        if (teamRules != null && teamRules.toleranceMeters() != null) {
            return teamRules.toleranceMeters();
        }
        if (siteRules != null && siteRules.toleranceMeters() != null) {
            return siteRules.toleranceMeters();
        }
        return defaultValue;
    }

    private static List<StrictModeHours> firstDefined(
        ValidationRules employeeRules,
        ValidationRules teamRules,
        ValidationRules siteRules
    ) {
        if (employeeRules != null && employeeRules.strictModeHours() != null) {
            return employeeRules.strictModeHours();
        }
        if (teamRules != null && teamRules.strictModeHours() != null) {
            return teamRules.strictModeHours();
        }
        if (siteRules != null && siteRules.strictModeHours() != null) {
            return siteRules.strictModeHours();
        }
        return List.of();
    }

    private static boolean firstDefined(
        ValidationRules employeeRules,
        ValidationRules teamRules,
        ValidationRules siteRules,
        boolean defaultValue
    ) {
        if (employeeRules != null && employeeRules.approvalRequired() != null) {
            return employeeRules.approvalRequired();
        }
        if (teamRules != null && teamRules.approvalRequired() != null) {
            return teamRules.approvalRequired();
        }
        if (siteRules != null && siteRules.approvalRequired() != null) {
            return siteRules.approvalRequired();
        }
        return defaultValue;
    }

    private static void requireUniqueTeamIds(List<Team> teams) {
        if (teams.stream().map(Team::id).distinct().count() != teams.size()) {
            throw new IllegalArgumentException("Site team IDs must be unique");
        }
    }

    private static void requireUniqueGeofenceIds(List<GeofenceCircle> geofences) {
        if (geofences.stream().map(GeofenceCircle::id).distinct().count() != geofences.size()) {
            throw new IllegalArgumentException("Site geofence IDs must be unique");
        }
    }

    private static void requireValidAssignments(List<Team> teams, List<SiteAssignment> assignments) {
        for (SiteAssignment assignment : assignments) {
            if (teams.stream().noneMatch(team -> team.id().equals(assignment.teamId()))) {
                throw new IllegalArgumentException("Assignment references a team outside this site");
            }
        }
    }

    private static void requireNonOverlappingAssignments(String siteId, List<SiteAssignment> assignments) {
        for (int first = 0; first < assignments.size(); first++) {
            for (int second = first + 1; second < assignments.size(); second++) {
                SiteAssignment firstAssignment = assignments.get(first);
                SiteAssignment secondAssignment = assignments.get(second);
                if (firstAssignment.employeeId().equals(secondAssignment.employeeId())
                    && rangesOverlap(
                        firstAssignment.effectiveFrom(), firstAssignment.effectiveTo(),
                        secondAssignment.effectiveFrom(), secondAssignment.effectiveTo()
                    )) {
                    throw new IllegalArgumentException(
                        "Employee " + firstAssignment.employeeId() + " has overlapping assignments at site " + siteId
                    );
                }
            }
        }
    }

    private static void requireNonOverlappingPrimaryGeofences(List<GeofenceCircle> geofences) {
        List<GeofenceCircle> primaryGeofences = geofences.stream().filter(GeofenceCircle::primary).toList();
        for (int first = 0; first < primaryGeofences.size(); first++) {
            for (int second = first + 1; second < primaryGeofences.size(); second++) {
                GeofenceCircle firstGeofence = primaryGeofences.get(first);
                GeofenceCircle secondGeofence = primaryGeofences.get(second);
                if (rangesOverlap(
                    firstGeofence.effectiveFrom(), firstGeofence.effectiveTo(),
                    secondGeofence.effectiveFrom(), secondGeofence.effectiveTo()
                )) {
                    throw new IllegalArgumentException("Site cannot have overlapping primary geofences");
                }
            }
        }
    }

    private static boolean rangesOverlap(
        LocalDate firstStart,
        LocalDate firstEnd,
        LocalDate secondStart,
        LocalDate secondEnd
    ) {
        return (firstEnd == null || !firstEnd.isBefore(secondStart))
            && (secondEnd == null || !secondEnd.isBefore(firstStart));
    }

    private static void requireIdentifier(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
    }
}
