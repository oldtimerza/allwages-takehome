package com.allwage.clockin.model;

import java.util.Objects;

/**
 * A team configured within a site.
 */
public record Team(String id, String name, ValidationRules validationRules) {

    /**
     * Creates a team with a site-local identifier and name.
     */
    public Team {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Team ID is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Team name is required");
        }
    }

    /**
     * Replaces this team's site-specific validation-rule overrides.
     *
     * @param rules replacement rule overrides
     * @return updated team
     */
    public Team withValidationRules(ValidationRules rules) {
        return new Team(id, name, Objects.requireNonNull(rules, "Team validation rules are required"));
    }
}
