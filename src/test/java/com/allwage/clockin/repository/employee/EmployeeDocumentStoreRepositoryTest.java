package com.allwage.clockin.repository.employee;

import com.allwage.clockin.model.employee.Employee;
import com.allwage.clockin.repository.store.DocumentStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDocumentStoreRepositoryTest {

    private final DocumentStore store = new DocumentStore();
    private final EmployeeDocumentStoreRepository repository = new EmployeeDocumentStoreRepository(store);

    @AfterEach
    void clearStore() {
        store.clearCollection("employees");
    }

    @Test
    void savesAndFindsEmployeeByIdentifier() {
        Employee employee = new Employee("employee-1", "Nomsa Dlamini", "+27115550123");

        repository.save(employee);

        assertThat(repository.findById("employee-1")).contains(employee);
    }

    @Test
    void savesEmployeeOnlyWhenItsIdentifierIsUnused() {
        // Given
        Employee original = new Employee("employee-1", "Nomsa Dlamini", "+27115550123");
        Employee replacement = new Employee("employee-1", "Thabo Molefe", "+27115550124");

        // When
        boolean firstCreated = repository.saveIfAbsent(original);
        boolean secondCreated = repository.saveIfAbsent(replacement);

        // Then
        assertThat(firstCreated).isTrue();
        assertThat(secondCreated).isFalse();
        assertThat(repository.findById("employee-1")).contains(original);
    }
}
