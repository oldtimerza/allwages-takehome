package com.allwage.clockin.repository.employee;

import com.allwage.clockin.model.Employee;
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
}
