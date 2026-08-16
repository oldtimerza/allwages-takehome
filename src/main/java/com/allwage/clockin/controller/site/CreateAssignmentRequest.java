package com.allwage.clockin.controller.site;

import com.allwage.clockin.model.ValidationRules;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Assigns an existing employee to a team at one site from a date.
 */
public record CreateAssignmentRequest(
    @NotBlank(message = "Employee ID is required")
    String employeeId,

    @NotNull(message = "Assignment effective start date is required")
    LocalDate effectiveFrom,

    @Valid ValidationRulesRequest validationRules
) {

    /**
     * Converts optional HTTP validation-rule overrides to the domain model.
     *
     * @return domain rule overrides, or null when the assignment inherits rules
     */
    public ValidationRules validationRulesModel() {
        return validationRules == null ? null : validationRules.toModel();
    }
}
