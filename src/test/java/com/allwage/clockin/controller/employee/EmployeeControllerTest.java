package com.allwage.clockin.controller.employee;

import com.allwage.clockin.model.employee.Employee;
import com.allwage.clockin.repository.store.DocumentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmployeeControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentStore store;

    @BeforeEach
    void setUp() {
        store.clearCollection("employees");
    }

    @Test
    void createsEmployee() {
        // Given
        String requestBody = """
            {
                "id": "employee-1",
                "name": "Ada Lovelace",
                "phoneNumber": "+27115550100"
            }
            """;

        // When
        ResponseEntity<EmployeeResponse> response = restTemplate.postForEntity(
            "/api/employees",
            new HttpEntity<>(requestBody, jsonHeaders()),
            EmployeeResponse.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(new EmployeeResponse("employee-1", "Ada Lovelace", "+27115550100"));
        assertThat(store.findById("employees", "employee-1", Employee.class)).contains(
            new Employee("employee-1", "Ada Lovelace", "+27115550100")
        );
    }

    @Test
    void rejectsDuplicateEmployeeWithoutOverwritingIt() {
        // Given
        store.save("employees", "employee-1", new Employee("employee-1", "Ada Lovelace", "+27115550100"));
        String requestBody = """
            {
                "id": "employee-1",
                "name": "Grace Hopper",
                "phoneNumber": "+27115550101"
            }
            """;

        // When
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/api/employees",
            new HttpEntity<>(requestBody, jsonHeaders()),
            Void.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(store.findById("employees", "employee-1", Employee.class)).contains(
            new Employee("employee-1", "Ada Lovelace", "+27115550100")
        );
    }

    @Test
    void rejectsEmployeeWithoutName() {
        // Given
        String requestBody = """
            {
                "id": "employee-1",
                "name": "",
                "phoneNumber": "+27115550100"
            }
            """;

        // When
        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/api/employees",
            new HttpEntity<>(requestBody, jsonHeaders()),
            Void.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(store.findById("employees", "employee-1", Employee.class)).isEmpty();
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
