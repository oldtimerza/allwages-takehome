package com.allwage.clockin.controller.site;

import com.allwage.clockin.model.Team;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Creates a team owned by one site.
 */
public record CreateTeamRequest(
    @NotBlank(message = "Team ID is required")
    String id,

    @NotBlank(message = "Team name is required")
    String name,

    @Valid ValidationRulesRequest validationRules
) {

    /**
     * Converts this HTTP contract to a site-owned team.
     *
     * @return domain team
     */
    public Team toModel() {
        return new Team(id, name, validationRules == null ? null : validationRules.toModel());
    }
}
