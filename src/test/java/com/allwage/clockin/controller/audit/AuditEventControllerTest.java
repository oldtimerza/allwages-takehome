package com.allwage.clockin.controller.audit;

import com.allwage.clockin.model.audit.AuditEvent;
import com.allwage.clockin.model.audit.AuditEventType;
import com.allwage.clockin.model.audit.AuditReasonCode;
import com.allwage.clockin.model.audit.AuditSource;
import com.allwage.clockin.model.audit.ClockAuditPayload;
import com.allwage.clockin.repository.store.DocumentStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditEventControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentStore store;

    @BeforeEach
    void setUp() {
        store.clearCollection("clock-audits");
    }

    @Test
    void findPage_returnsNewestEntriesWithMetadataAndFilter() {
        saveAudit("audit-1", "2026-08-16T10:00:00Z", AuditEventType.CLOCK_ACCEPTED);
        saveAudit("audit-2", "2026-08-16T11:00:00Z", AuditEventType.CLOCK_REJECTED);
        saveAudit("audit-3", "2026-08-16T12:00:00Z", AuditEventType.CLOCK_REJECTED);

        ResponseEntity<AuditEventPageResponse> response = restTemplate.exchange(
            "/api/audit-events?page=0&size=1&type=CLOCK_REJECTED",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() { }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isNotBlank();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().entries()).hasSize(1);
        AuditEventResponse entry = response.getBody().entries().getFirst();
        assertThat(entry.id()).isEqualTo("audit-3");
        assertThat(entry.reasonCode()).isEqualTo(AuditReasonCode.GEOFENCE_REJECTED);
        assertThat(entry.source()).isEqualTo(AuditSource.MOBILE_API);
        assertThat(entry.httpStatus()).isEqualTo(200);
        assertThat(response.getBody().page()).isZero();
        assertThat(response.getBody().size()).isOne();
        assertThat(response.getBody().totalElements()).isEqualTo(2);
        assertThat(response.getBody().totalPages()).isEqualTo(2);
    }

    @Test
    void findPage_rejectsInvalidParametersAndReturnsEmptyOutOfRangePage() {
        saveAudit("audit-1", "2026-08-16T10:00:00Z", AuditEventType.CLOCK_ACCEPTED);

        ResponseEntity<Void> invalidResponse = restTemplate.getForEntity("/api/audit-events?page=-1", Void.class);
        ResponseEntity<AuditEventPageResponse> beyondResultsResponse = restTemplate.exchange(
            "/api/audit-events?page=3&size=1",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<>() { }
        );

        assertThat(invalidResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(beyondResultsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(beyondResultsResponse.getBody()).isNotNull();
        assertThat(beyondResultsResponse.getBody().entries()).isEmpty();
        assertThat(beyondResultsResponse.getBody().totalElements()).isOne();
        assertThat(beyondResultsResponse.getBody().totalPages()).isOne();
    }

    private void saveAudit(String id, String occurredAt, AuditEventType type) {
        store.save("clock-audits", id, new AuditEvent(
            id,
            Instant.parse(occurredAt),
            "correlation-123",
            "clock-123",
            "emp-123",
            type,
            1,
            new ClockAuditPayload(AuditReasonCode.GEOFENCE_REJECTED, AuditSource.MOBILE_API, 200)
        ));
    }
}
