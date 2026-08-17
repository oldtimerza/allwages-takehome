package com.allwage.clockin.controller.clock;

import com.allwage.clockin.model.ClockEvent;
import com.allwage.clockin.model.ClockValidationResult;
import com.allwage.clockin.model.Employee;
import com.allwage.clockin.model.GeoCoordinate;
import com.allwage.clockin.model.GeofenceCircle;
import com.allwage.clockin.model.Site;
import com.allwage.clockin.model.SiteAssignment;
import com.allwage.clockin.model.Team;
import com.allwage.clockin.model.ValidatedClockEvent;
import com.allwage.clockin.repository.store.DocumentStore;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ManagerClockControllerTest {

    private static final String EMPLOYEE_ID = "employee-1";
    private static final String SITE_ID = "site-1";
    private static final String HISTORICAL_TEAM_ID = "team-historical";
    private static final String CURRENT_TEAM_ID = "team-current";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentStore store;

    @BeforeEach
    void setUp() {
        store.clearCollection("clocks");
        store.clearCollection("clock-audits");
        store.clearCollection("employees");
        store.clearCollection("sites");
    }

    @Test
    void findEmployeeClocks_returnsNewestPageWithMetadata() {
        // Given
        saveEmployee();
        saveClock("clock-1", EMPLOYEE_ID, "2026-08-16T09:00:00+02:00", accepted());
        saveClock("clock-2", EMPLOYEE_ID, "2026-08-16T11:00:00+02:00", rejected());
        saveClock("clock-3", EMPLOYEE_ID, "2026-08-16T10:00:00+02:00", accepted());
        saveClock("clock-other", "employee-2", "2026-08-16T12:00:00+02:00", accepted());

        // When
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
            "/api/employees/{employeeId}/clocks?page=0&size=2",
            JsonNode.class,
            EMPLOYEE_ID
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("entries")).hasSize(2);
        assertThat(response.getBody().at("/entries/0/clockEvent/id").asText()).isEqualTo("clock-2");
        assertThat(response.getBody().at("/entries/1/clockEvent/id").asText()).isEqualTo("clock-3");
        assertThat(response.getBody().path("page").asInt()).isZero();
        assertThat(response.getBody().path("size").asInt()).isEqualTo(2);
        assertThat(response.getBody().path("totalElements").asInt()).isEqualTo(3);
        assertThat(response.getBody().path("totalPages").asInt()).isEqualTo(2);
    }

    @Test
    void findSiteAndTeamClocks_usesHistoricalAssignmentsForRejectedAttempts() {
        // Given
        saveEmployee();
        saveSite();
        saveClock("clock-accepted", EMPLOYEE_ID, "2026-08-16T09:00:00+02:00", accepted());
        saveClock("clock-rejected", EMPLOYEE_ID, "2026-08-16T10:00:00+02:00", rejected());

        // When
        ResponseEntity<JsonNode> siteResponse = restTemplate.getForEntity(
            "/api/sites/{siteId}/clocks",
            JsonNode.class,
            SITE_ID
        );
        ResponseEntity<JsonNode> historicalTeamResponse = restTemplate.getForEntity(
            "/api/sites/{siteId}/teams/{teamId}/clocks",
            JsonNode.class,
            SITE_ID,
            HISTORICAL_TEAM_ID
        );
        ResponseEntity<JsonNode> currentTeamResponse = restTemplate.getForEntity(
            "/api/sites/{siteId}/teams/{teamId}/clocks",
            JsonNode.class,
            SITE_ID,
            CURRENT_TEAM_ID
        );

        // Then
        assertThat(siteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(siteResponse.getBody()).isNotNull();
        assertThat(siteResponse.getBody().path("entries")).hasSize(2);
        assertThat(siteResponse.getBody().path("page").asInt()).isZero();
        assertThat(siteResponse.getBody().path("size").asInt()).isEqualTo(50);
        assertThat(historicalTeamResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(historicalTeamResponse.getBody()).isNotNull();
        assertThat(historicalTeamResponse.getBody().path("entries")).hasSize(2);
        assertThat(historicalTeamResponse.getBody().at("/entries/0/clockEvent/id").asText())
            .isEqualTo("clock-rejected");
        assertThat(currentTeamResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(currentTeamResponse.getBody()).isNotNull();
        assertThat(currentTeamResponse.getBody().path("entries")).isEmpty();
    }

    @Test
    void findClocks_filtersEmployeeSiteAndTeamPagesByStatus() {
        // Given
        saveEmployee();
        saveSite();
        saveClock("clock-accepted", EMPLOYEE_ID, "2026-08-16T09:00:00+02:00", accepted());
        saveClock("clock-rejected", EMPLOYEE_ID, "2026-08-16T10:00:00+02:00", rejected());

        // When
        ResponseEntity<JsonNode> employeeResponse = restTemplate.getForEntity(
            "/api/employees/{employeeId}/clocks?status=REJECTED",
            JsonNode.class,
            EMPLOYEE_ID
        );
        ResponseEntity<JsonNode> siteResponse = restTemplate.getForEntity(
            "/api/sites/{siteId}/clocks?status=ACCEPTED",
            JsonNode.class,
            SITE_ID
        );
        ResponseEntity<JsonNode> teamResponse = restTemplate.getForEntity(
            "/api/sites/{siteId}/teams/{teamId}/clocks?status=REJECTED",
            JsonNode.class,
            SITE_ID,
            HISTORICAL_TEAM_ID
        );

        // Then
        assertThat(employeeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(employeeResponse.getBody()).isNotNull();
        assertThat(employeeResponse.getBody().path("entries")).hasSize(1);
        assertThat(employeeResponse.getBody().at("/entries/0/validationResult/decision").asText()).isEqualTo("REJECTED");
        assertThat(employeeResponse.getBody().path("totalElements").asInt()).isOne();
        assertThat(siteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(siteResponse.getBody()).isNotNull();
        assertThat(siteResponse.getBody().path("entries")).hasSize(1);
        assertThat(siteResponse.getBody().at("/entries/0/validationResult/decision").asText()).isEqualTo("ACCEPTED");
        assertThat(teamResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(teamResponse.getBody()).isNotNull();
        assertThat(teamResponse.getBody().path("entries")).hasSize(1);
        assertThat(teamResponse.getBody().at("/entries/0/validationResult/decision").asText()).isEqualTo("REJECTED");
    }

    @Test
    void findClocks_rejectsInvalidPagingAndReturnsNotFoundForUnknownResources() {
        // Given
        saveEmployee();
        saveSite();

        // When
        ResponseEntity<Void> invalidPagingResponse = restTemplate.getForEntity(
            "/api/employees/{employeeId}/clocks?page=-1",
            Void.class,
            EMPLOYEE_ID
        );
        ResponseEntity<Void> zeroSizeResponse = restTemplate.getForEntity(
            "/api/employees/{employeeId}/clocks?size=0",
            Void.class,
            EMPLOYEE_ID
        );
        ResponseEntity<Void> excessiveSizeResponse = restTemplate.getForEntity(
            "/api/employees/{employeeId}/clocks?size=101",
            Void.class,
            EMPLOYEE_ID
        );
        ResponseEntity<Void> invalidStatusResponse = restTemplate.getForEntity(
            "/api/employees/{employeeId}/clocks?status=UNKNOWN",
            Void.class,
            EMPLOYEE_ID
        );
        ResponseEntity<Void> unknownEmployeeResponse = restTemplate.getForEntity(
            "/api/employees/unknown/clocks",
            Void.class
        );
        ResponseEntity<Void> unknownSiteResponse = restTemplate.getForEntity(
            "/api/sites/unknown/clocks",
            Void.class
        );
        ResponseEntity<Void> unknownTeamResponse = restTemplate.getForEntity(
            "/api/sites/{siteId}/teams/unknown/clocks",
            Void.class,
            SITE_ID
        );

        // Then
        assertThat(invalidPagingResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(zeroSizeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(excessiveSizeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalidStatusResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unknownEmployeeResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownSiteResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(unknownTeamResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void saveEmployee() {
        store.save("employees", EMPLOYEE_ID, new Employee(EMPLOYEE_ID, "Employee One", "+27115550100"));
    }

    private void saveSite() {
        store.save("sites", SITE_ID, new Site(
            SITE_ID,
            "Site One",
            null,
            List.of(new Team(HISTORICAL_TEAM_ID, "Historical Team", null), new Team(CURRENT_TEAM_ID, "Current Team", null)),
            List.of(new GeofenceCircle(
                "geofence-1",
                new GeoCoordinate(-26.2041, 28.0473),
                100,
                true,
                LocalDate.of(2026, 1, 1),
                null
            )),
            List.of(
                new SiteAssignment(
                    EMPLOYEE_ID,
                    HISTORICAL_TEAM_ID,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 8, 16),
                    null
                ),
                new SiteAssignment(EMPLOYEE_ID, CURRENT_TEAM_ID, LocalDate.of(2026, 8, 17), null, null)
            )
        ));
    }

    private void saveClock(
        String id,
        String employeeId,
        String timestamp,
        ClockValidationResult validationResult
    ) {
        ClockEvent event = new ClockEvent(
            id,
            employeeId,
            ZonedDateTime.parse(timestamp),
            -26.2041,
            28.0473,
            10,
            ClockEvent.ClockType.IN
        );
        store.save("clocks", id, new ValidatedClockEvent(event, validationResult));
    }

    private ClockValidationResult accepted() {
        return new ClockValidationResult(
            ClockValidationResult.Decision.ACCEPTED,
            ClockValidationResult.Reason.ACCEPTED,
            SITE_ID,
            "geofence-1"
        );
    }

    private ClockValidationResult rejected() {
        return new ClockValidationResult(
            ClockValidationResult.Decision.REJECTED,
            ClockValidationResult.Reason.OUTSIDE_GEOFENCE,
            null,
            null
        );
    }
}
