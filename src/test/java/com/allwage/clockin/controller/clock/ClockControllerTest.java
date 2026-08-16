package com.allwage.clockin.controller.clock;

import com.allwage.clockin.model.AuditEvent;
import com.allwage.clockin.model.AuditEventType;
import com.allwage.clockin.model.AuditReasonCode;
import com.allwage.clockin.model.ClockAuditPayload;
import com.allwage.clockin.model.ClockEvent;
import com.allwage.clockin.repository.store.DocumentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClockControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentStore store;

    @BeforeEach
    void setUp() {
        store.clearCollection("clocks");
        store.clearCollection("clock-audits");
    }

    @Test
    void clockIn_savesToStore() {
        String requestBody = """
            {
                "employeeId": "emp-123",
                "timestamp": "2024-01-15T09:00:00+02:00",
                "latitude": -26.2041,
                "longitude": 28.0473,
                "accuracyMeters": 10.0,
                "type": "IN"
            }
            """;

        ResponseEntity<ClockEvent> response = restTemplate.postForEntity(
            "/api/clocks",
            new org.springframework.http.HttpEntity<>(
                requestBody,
                createJsonHeaders()
            ),
            ClockEvent.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().employeeId()).isEqualTo("emp-123");
        assertThat(response.getBody().type()).isEqualTo(ClockEvent.ClockType.IN);

        // Verify it was saved to the store
        List<ClockEvent> stored = store.findAll("clocks", ClockEvent.class);
        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst().employeeId()).isEqualTo("emp-123");

        List<AuditEvent> audits = store.findAll("clock-audits", AuditEvent.class);
        assertThat(audits).hasSize(1);
        AuditEvent audit = audits.getFirst();
        assertThat(audit.type()).isEqualTo(AuditEventType.CLOCK_ACCEPTED);
        assertThat(audit.clockEventId()).isEqualTo(response.getBody().id());
        assertThat(audit.employeeId()).isEqualTo("emp-123");
        assertThat(audit.correlationId()).isEqualTo(response.getHeaders().getFirst("X-Correlation-Id"));
        assertThat(audit.payload()).isInstanceOf(ClockAuditPayload.class);
        ClockAuditPayload payload = (ClockAuditPayload) audit.payload();
        assertThat(payload.reasonCode()).isEqualTo(AuditReasonCode.CLOCK_ACCEPTED);
    }

    @Test
    void invalidClockRequest_recordsRejectionAudit() {
        String requestBody = """
            {
                "employeeId": "emp-123"
            }
            """;

        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/api/clocks",
            new org.springframework.http.HttpEntity<>(
                requestBody,
                createJsonHeaders()
            ),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(store.findAll("clocks", ClockEvent.class)).isEmpty();
        List<AuditEvent> audits = store.findAll("clock-audits", AuditEvent.class);
        assertThat(audits).hasSize(1);
        assertThat(audits.getFirst().type()).isEqualTo(AuditEventType.CLOCK_REJECTED);
        assertThat(audits.getFirst().payload()).isInstanceOf(ClockAuditPayload.class);
        ClockAuditPayload payload = (ClockAuditPayload) audits.getFirst().payload();
        assertThat(payload.reasonCode()).isEqualTo(AuditReasonCode.REQUEST_VALIDATION_FAILED);
    }

    private org.springframework.http.HttpHeaders createJsonHeaders() {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }
}
