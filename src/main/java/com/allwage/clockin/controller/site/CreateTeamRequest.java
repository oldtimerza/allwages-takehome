package com.allwage.clockin.controller.site;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Creates a team owned by one site.
 */
public record CreateTeamRequest(
    @NotBlank(message = "Team name is required")
    String name,

    @Valid ValidationRulesRequest validationRules
) {

}
