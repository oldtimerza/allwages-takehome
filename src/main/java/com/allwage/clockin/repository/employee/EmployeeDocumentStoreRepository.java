package com.allwage.clockin.repository.employee;

import com.allwage.clockin.model.employee.Employee;
import com.allwage.clockin.repository.store.DocumentStore;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Document-store implementation of employee persistence.
 */
@Repository
public class EmployeeDocumentStoreRepository implements EmployeeRepository {

    private static final String COLLECTION = "employees";

    private final DocumentStore store;

    public EmployeeDocumentStoreRepository(@NonNull DocumentStore store) {
        this.store = store;
    }

    @Override
    public void save(@NonNull Employee employee) {
        store.save(COLLECTION, employee.id(), employee);
    }

    @Override
    public boolean saveIfAbsent(@NonNull Employee employee) {
        try {
            store.saveIfAbsent(COLLECTION, employee.id(), employee);
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    @Override
    public @NonNull Optional<Employee> findById(@NonNull String id) {
        return store.findById(COLLECTION, id, Employee.class);
    }
}
