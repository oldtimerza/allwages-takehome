package com.allwage.clockin.controller.site;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Full replacement of validation-rule overrides for one dated employee assignment at a site.
 */
public record EmployeeValidationRulesRequest(
    @NotNull(message = "Assignment effective start date is required")
    LocalDate effectiveFrom,

    @NotNull(message = "Validation rules are required")
    @Valid ValidationRulesRequest validationRules
) { }
