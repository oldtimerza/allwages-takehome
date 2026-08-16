package com.allwage.clockin.controller.site;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Creates a site aggregate with no teams, geofences, or employee assignments.
 */
public record CreateSiteRequest(
    @NotBlank(message = "Site ID is required")
    String id,

    @NotBlank(message = "Site name is required")
    String name,

    @Valid ValidationRulesRequest validationRules
) { }
