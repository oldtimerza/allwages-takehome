package com.allwage.clockin.controller.employee;

import com.allwage.clockin.model.employee.Employee;
import com.allwage.clockin.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for employee registration.
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(@NonNull EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Creates an employee with a server-generated identifier.
     *
     * @param request employee details
     * @return created employee
     */
    @PostMapping
    public @NonNull ResponseEntity<EmployeeResponse> createEmployee(
        @Valid @RequestBody CreateEmployeeRequest request
    ) {
        Employee employee = new Employee(UUID.randomUUID().toString(), request.name(), request.phoneNumber());
        return employeeService.createEmployee(employee)
            .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(employeeResponse(created)))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    private static EmployeeResponse employeeResponse(Employee employee) {
        return new EmployeeResponse(employee.id(), employee.name(), employee.phoneNumber());
    }
}
