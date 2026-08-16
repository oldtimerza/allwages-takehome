package com.allwage.clockin.service;

import com.allwage.clockin.model.Employee;
import com.allwage.clockin.repository.employee.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Coordinates employee registration.
 */
@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeService(@NonNull EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Creates an employee when its identifier is not already registered.
     *
     * @param employee employee to register
     * @return the registered employee, or empty when the identifier already exists
     */
    public @NonNull Optional<Employee> createEmployee(@NonNull Employee employee) {
        if (!employeeRepository.saveIfAbsent(employee)) {
            log.warn("Cannot create employee because employeeId={} already exists", employee.id());
            return Optional.empty();
        }
        log.info("Created employee: employeeId={}", employee.id());
        return Optional.of(employee);
    }
}
