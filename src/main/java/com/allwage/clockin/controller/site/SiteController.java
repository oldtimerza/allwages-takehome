package com.allwage.clockin.controller.site;

import com.allwage.clockin.model.Site.GeoCoordinate;
import com.allwage.clockin.model.Site.GeofenceCircle;
import com.allwage.clockin.model.Site.Site;
import com.allwage.clockin.model.Site.SiteAssignment;
import com.allwage.clockin.model.Site.Team;
import com.allwage.clockin.model.Site.ValidationRules;
import com.allwage.clockin.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing site-owned validation-rule configurations.
 */
@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(@NonNull SiteService siteService) {
        this.siteService = siteService;
    }

    /**
     * Creates an empty site aggregate.
     *
     * @param request site details
     * @return created site, or conflict when the identifier is already in use
     */
    @PostMapping
    public @NonNull ResponseEntity<SiteResponse> createSite(@Valid @RequestBody CreateSiteRequest request) {
        Site site = new Site(
            request.id(),
            request.name(),
            request.validationRules() == null ? null : new ValidationRules(
                request.validationRules().toleranceMeters(),
                request.validationRules().approvalRequired(),
                request.validationRules().strictModeHours()
            ),
            List.of(),
            List.of(),
            List.of()
        );
        return siteService.createSite(site)
            .map(createdSite -> ResponseEntity.status(HttpStatus.CREATED).body(siteResponse(createdSite)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    /**
     * Adds a geofence owned by a site.
     *
     * @param siteId site identifier
     * @param request geofence details
     * @return created geofence, or not found when the site does not exist
     */
    @PostMapping("/{siteId}/geofences")
    public @NonNull ResponseEntity<GeofenceResponse> addGeofence(
        @PathVariable String siteId,
        @Valid @RequestBody CreateGeofenceRequest request
    ) {
        GeofenceCircle geofence = new GeofenceCircle(
            request.id(),
            new GeoCoordinate(request.latitude(), request.longitude()),
            request.radiusMeters(),
            request.primary(),
            request.effectiveFrom(),
            request.effectiveTo()
        );
        return siteService.addGeofence(siteId, geofence)
            .map(createdGeofence -> ResponseEntity.status(HttpStatus.CREATED).body(geofenceResponse(createdGeofence)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Adds a team owned by a site.
     *
     * @param siteId site identifier
     * @param request team to add
     * @return created team, or not found when the site does not exist
     */
    @PostMapping("/{siteId}/teams")
    public @NonNull ResponseEntity<Team> addTeam(
        @PathVariable String siteId,
        @Valid @RequestBody CreateTeamRequest request
    ) {
        return siteService.addTeam(siteId, request.toModel())
            .map(team -> ResponseEntity.status(HttpStatus.CREATED).body(team))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Assigns an employee to a team owned by a site.
     *
     * @param siteId site identifier
     * @param teamId site-local team identifier
     * @param request employee assignment details
     * @return created assignment, or not found when an owner or reference does not exist
     */
    @PostMapping("/{siteId}/teams/{teamId}/assignments")
    public @NonNull ResponseEntity<SiteAssignment> assignEmployee(
        @PathVariable String siteId,
        @PathVariable String teamId,
        @Valid @RequestBody CreateAssignmentRequest request
    ) {
        return siteService.assignEmployee(
            siteId,
            teamId,
            request.employeeId(),
            request.effectiveFrom(),
            request.validationRulesModel()
        )
            .map(assignment -> ResponseEntity.status(HttpStatus.CREATED).body(assignment))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Replaces the default validation-rule overrides at a site.
     *
     * @param siteId site identifier
     * @param request replacement rules
     * @return saved rules, or not found when the site does not exist
     */
    @PutMapping("/{siteId}/validation-rules")
    public @NonNull ResponseEntity<ValidationRules> replaceSiteValidationRules(
        @PathVariable String siteId,
        @Valid @RequestBody ValidationRulesRequest request
    ) {
        return response(siteService.replaceSiteValidationRules(siteId, request.toModel()));
    }

    /**
     * Replaces validation-rule overrides for a team within a site.
     *
     * @param siteId site identifier
     * @param teamId site-local team identifier
     * @param request replacement rules
     * @return saved rules, or not found when the site or team does not exist
     */
    @PutMapping("/{siteId}/teams/{teamId}/validation-rules")
    public @NonNull ResponseEntity<ValidationRules> replaceTeamValidationRules(
        @PathVariable String siteId,
        @PathVariable String teamId,
        @Valid @RequestBody ValidationRulesRequest request
    ) {
        return response(siteService.replaceTeamValidationRules(siteId, teamId, request.toModel()));
    }

    /**
     * Replaces validation-rule overrides for one dated employee assignment at a site.
     *
     * @param siteId site identifier
     * @param employeeId employee identifier
     * @param request assignment identifier and replacement rules
     * @return saved rules, or not found when the site or assignment does not exist
     */
    @PutMapping("/{siteId}/employees/{employeeId}/validation-rules")
    public @NonNull ResponseEntity<ValidationRules> replaceAssignmentValidationRules(
        @PathVariable String siteId,
        @PathVariable String employeeId,
        @Valid @RequestBody EmployeeValidationRulesRequest request
    ) {
        return response(siteService.replaceAssignmentValidationRules(
            siteId,
            employeeId,
            request.effectiveFrom(),
            request.validationRules().toModel()
        ));
    }

    private static ResponseEntity<ValidationRules> response(java.util.Optional<ValidationRules> rules) {
        return rules.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static SiteResponse siteResponse(Site site) {
        return new SiteResponse(site.id(), site.name());
    }

    private static GeofenceResponse geofenceResponse(GeofenceCircle geofence) {
        return new GeofenceResponse(
            geofence.id(),
            geofence.centre().latitude(),
            geofence.centre().longitude(),
            geofence.radiusMeters(),
            geofence.primary(),
            geofence.effectiveFrom(),
            geofence.effectiveTo()
        );
    }
}
