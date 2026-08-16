package com.allwage.clockin.model;

/**
 * Represents an employee who can clock in/out at job sites.
 */
public record Employee(
    String id,
    String name,
    String phoneNumber
) {

    /**
     * Creates an employee with a stable identifier and contact details.
     */
    public Employee {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Employee ID is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Employee name is required");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Employee phone number is required");
        }
    }
}
