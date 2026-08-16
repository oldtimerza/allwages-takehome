package com.allwage.clockin.repository.employee;

import com.allwage.clockin.model.Employee;

import java.util.Optional;

/**
 * Persistence port for employee documents.
 */
public interface EmployeeRepository {

    /**
     * Saves an employee document.
     *
     * @param employee employee to save
     */
    void save(Employee employee);

    /**
     * Finds an employee by their stable identifier.
     *
     * @param id employee identifier
     * @return the employee when present
     */
    Optional<Employee> findById(String id);
}
