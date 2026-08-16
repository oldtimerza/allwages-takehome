package com.allwage.clockin.service;

import com.allwage.clockin.model.Site;
import com.allwage.clockin.model.ValidationRules;
import com.allwage.clockin.repository.site.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Coordinates updates to site-owned validation-rule configurations.
 */
@Service
public class SiteService {

    private static final Logger log = LoggerFactory.getLogger(SiteService.class);

    private final SiteRepository siteRepository;

    public SiteService(@NonNull SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
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
