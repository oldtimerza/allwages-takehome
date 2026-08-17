package com.allwage.clockin.controller.site;

import com.allwage.clockin.model.Site.StrictModeHours;
import com.allwage.clockin.model.Site.ValidationRules;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Full replacement of validation-rule overrides at one site configuration scope.
 */
public record ValidationRulesRequest(
    @Min(value = 0, message = "Geofence tolerance cannot be negative")
    Integer toleranceMeters,

    Boolean approvalRequired,

    List<@NotNull(message = "Strict-mode hours are required") @Valid StrictModeHours> strictModeHours
) {

    /**
     * Converts this HTTP contract to a domain rule override set.
     *
     * @return domain rule overrides
     */
    public ValidationRules toModel() {
        return new ValidationRules(toleranceMeters, approvalRequired, strictModeHours);
    }
}
