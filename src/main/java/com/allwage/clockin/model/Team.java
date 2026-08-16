package com.allwage.clockin.model;

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
}
