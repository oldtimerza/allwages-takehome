package com.allwage.clockin.controller.employee;

import jakarta.validation.constraints.NotBlank;

/**
 * Creates an employee who can be assigned to a site team.
 */
public record CreateEmployeeRequest(
    @NotBlank(message = "Employee ID is required")
    String id,

    @NotBlank(message = "Employee name is required")
    String name,

    @NotBlank(message = "Employee phone number is required")
    String phoneNumber
) { }
