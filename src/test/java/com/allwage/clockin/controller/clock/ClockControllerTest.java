package com.allwage.clockin.controller.clock;

import com.allwage.clockin.model.AuditEvent;
import com.allwage.clockin.model.AuditEventType;
import com.allwage.clockin.model.AuditReasonCode;
import com.allwage.clockin.model.ClockAuditPayload;
import com.allwage.clockin.model.ClockEvent;
import com.allwage.clockin.model.ClockValidationResult;
import com.allwage.clockin.model.Employee;
import com.allwage.clockin.model.GeoCoordinate;
import com.allwage.clockin.model.GeofenceCircle;
import com.allwage.clockin.model.Site;
import com.allwage.clockin.model.SiteAssignment;
import com.allwage.clockin.model.Team;
import com.allwage.clockin.model.ValidationRules;
import com.allwage.clockin.model.ValidatedClockEvent;
import com.allwage.clockin.repository.store.DocumentStore;
import com.allwage.clockin.service.ClockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClockControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentStore store;

    @Autowired
    private ClockService clockService;

    @BeforeEach
    void setUp() {
        store.clearCollection("clocks");
        store.clearCollection("clock-audits");
        store.clearCollection("employees");
        store.clearCollection("sites");
    }

    @Test
    void clockIn_savesToStore() {
        configureEmployeeAndSite();
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

        ResponseEntity<ClockResponse> response = restTemplate.postForEntity(
            "/api/clocks",
            new org.springframework.http.HttpEntity<>(
                requestBody,
                createJsonHeaders()
            ),
            ClockResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().clockEvent().employeeId()).isEqualTo("emp-123");
        assertThat(response.getBody().clockEvent().type()).isEqualTo(ClockEvent.ClockType.IN);
        assertThat(response.getBody().validationResult().decision())
            .isEqualTo(ClockValidationResult.Decision.ACCEPTED);

        // Verify it was saved to the store
        List<ValidatedClockEvent> stored = store.findAll("clocks", ValidatedClockEvent.class);
        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst().clockEvent().employeeId()).isEqualTo("emp-123");

        ResponseEntity<ClockResponse> byIdResponse = restTemplate.getForEntity(
            "/api/clocks/{id}",
            ClockResponse.class,
            response.getBody().clockEvent().id()
        );
        ResponseEntity<ClockResponse[]> allResponse = restTemplate.getForEntity(
            "/api/clocks",
            ClockResponse[].class
        );
        assertThat(byIdResponse.getBody()).isNotNull();
        assertThat(byIdResponse.getBody().clockEvent()).isEqualTo(response.getBody().clockEvent());
        assertThat(byIdResponse.getBody().validationResult()).isEqualTo(response.getBody().validationResult());
        assertThat(allResponse.getBody()).containsExactly(response.getBody());

        List<AuditEvent> audits = store.findAll("clock-audits", AuditEvent.class);
        assertThat(audits).hasSize(1);
        AuditEvent audit = audits.getFirst();
        assertThat(audit.type()).isEqualTo(AuditEventType.CLOCK_ACCEPTED);
        assertThat(audit.clockEventId()).isEqualTo(response.getBody().clockEvent().id());
        assertThat(audit.employeeId()).isEqualTo("emp-123");
        assertThat(audit.correlationId()).isEqualTo(response.getHeaders().getFirst("X-Correlation-Id"));
        assertThat(audit.payload()).isInstanceOf(ClockAuditPayload.class);
        ClockAuditPayload payload = (ClockAuditPayload) audit.payload();
        assertThat(payload.reasonCode()).isEqualTo(AuditReasonCode.CLOCK_ACCEPTED);
    }

    @Test
    void clockOutsideGeofence_savesRejectedResultAndRejectionAudit() {
        configureEmployeeAndSite();
        String requestBody = """
            {
                "employeeId": "emp-123",
                "timestamp": "2024-01-15T09:00:00+02:00",
                "latitude": -26.3041,
                "longitude": 28.0473,
                "accuracyMeters": 10.0,
                "type": "IN"
            }
            """;

        ResponseEntity<ClockResponse> response = restTemplate.postForEntity(
            "/api/clocks",
            new org.springframework.http.HttpEntity<>(requestBody, createJsonHeaders()),
            ClockResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().validationResult().reason())
            .isEqualTo(ClockValidationResult.Reason.OUTSIDE_GEOFENCE);
        assertThat(store.findAll("clocks", ValidatedClockEvent.class)).hasSize(1);
        List<AuditEvent> audits = store.findAll("clock-audits", AuditEvent.class);
        assertThat(audits).hasSize(1);
        assertThat(audits.getFirst().type()).isEqualTo(AuditEventType.CLOCK_REJECTED);
        ClockAuditPayload payload = (ClockAuditPayload) audits.getFirst().payload();
        assertThat(payload.reasonCode()).isEqualTo(AuditReasonCode.GEOFENCE_REJECTED);
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
        assertThat(store.findAll("clocks", ValidatedClockEvent.class)).isEmpty();
        List<AuditEvent> audits = store.findAll("clock-audits", AuditEvent.class);
        assertThat(audits).hasSize(1);
        assertThat(audits.getFirst().type()).isEqualTo(AuditEventType.CLOCK_REJECTED);
        assertThat(audits.getFirst().payload()).isInstanceOf(ClockAuditPayload.class);
        ClockAuditPayload payload = (ClockAuditPayload) audits.getFirst().payload();
        assertThat(payload.reasonCode()).isEqualTo(AuditReasonCode.REQUEST_VALIDATION_FAILED);
    }

    @Test
    void invalidCoordinate_recordsRejectionAuditWithoutPersistingAClock() {
        String requestBody = """
            {
                "employeeId": "emp-123",
                "timestamp": "2024-01-15T09:00:00+02:00",
                "latitude": 91,
                "longitude": 28.0473,
                "accuracyMeters": 10.0,
                "type": "IN"
            }
            """;

        ResponseEntity<Void> response = restTemplate.postForEntity(
            "/api/clocks",
            new org.springframework.http.HttpEntity<>(requestBody, createJsonHeaders()),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(store.findAll("clocks", ValidatedClockEvent.class)).isEmpty();
        List<AuditEvent> audits = store.findAll("clock-audits", AuditEvent.class);
        assertThat(audits).hasSize(1);
        assertThat(audits.getFirst().type()).isEqualTo(AuditEventType.CLOCK_REJECTED);
        ClockAuditPayload payload = (ClockAuditPayload) audits.getFirst().payload();
        assertThat(payload.reasonCode()).isEqualTo(AuditReasonCode.REQUEST_VALIDATION_FAILED);
    }

    @Test
    void serviceFailure_recordsFailureAuditWithoutPersistingAClock() {
        configureEmployeeAndSite();
        ClockEvent invalidClock = new ClockEvent(
            "invalid-clock",
            "emp-123",
            java.time.ZonedDateTime.parse("2024-01-15T09:00:00+02:00"),
            91,
            28.0473,
            10,
            ClockEvent.ClockType.IN
        );

        assertThatIllegalArgumentException().isThrownBy(() -> clockService.processClock(invalidClock));
        assertThat(store.findAll("clocks", ValidatedClockEvent.class)).isEmpty();
        List<AuditEvent> audits = store.findAll("clock-audits", AuditEvent.class);
        assertThat(audits).hasSize(1);
        assertThat(audits.getFirst().type()).isEqualTo(AuditEventType.CLOCK_FAILED);
        ClockAuditPayload payload = (ClockAuditPayload) audits.getFirst().payload();
        assertThat(payload.reasonCode()).isEqualTo(AuditReasonCode.CLOCK_PROCESSING_FAILED);
    }

    private org.springframework.http.HttpHeaders createJsonHeaders() {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private void configureEmployeeAndSite() {
        store.save("employees", "emp-123", new Employee("emp-123", "Employee", "+27115550100"));
        store.save("sites", "site-1", new Site(
            "site-1",
            "Farm Alpha",
            new ValidationRules(20, false, null),
            List.of(new Team("team-1", "Harvest", null)),
            List.of(new GeofenceCircle(
                "zone-1",
                new GeoCoordinate(-26.2041, 28.0473),
                100,
                true,
                LocalDate.of(2024, 1, 1),
                null
            )),
            List.of(new SiteAssignment("emp-123", "team-1", LocalDate.of(2024, 1, 1), null, null))
        ));
    }
}
