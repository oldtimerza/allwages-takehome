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

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).matches(UUID_PATTERN);
        assertThat(response.getBody().name()).isEqualTo("Ada Lovelace");
        assertThat(response.getBody().phoneNumber()).isEqualTo("+27115550100");
        assertThat(store.findById("employees", response.getBody().id(), Employee.class)).contains(
            new Employee(response.getBody().id(), "Ada Lovelace", "+27115550100")
        );
    }

    @Test
    void rejectsEmployeeWithoutName() {
        // Given
        String requestBody = """
            {
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
        assertThat(store.findAll("employees", Employee.class)).isEmpty();
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
