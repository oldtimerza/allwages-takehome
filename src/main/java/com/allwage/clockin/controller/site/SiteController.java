package com.allwage.clockin.controller.site;

import com.allwage.clockin.model.ValidationRules;
import com.allwage.clockin.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
