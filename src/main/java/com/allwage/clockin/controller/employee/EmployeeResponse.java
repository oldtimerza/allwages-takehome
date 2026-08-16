package com.allwage.clockin.controller.employee;

/**
 * Public representation of an employee.
 */
public record EmployeeResponse(
    String id,
    String name,
    String phoneNumber
) { }
