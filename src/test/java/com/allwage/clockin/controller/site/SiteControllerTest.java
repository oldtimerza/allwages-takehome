package com.allwage.clockin.controller.site;

import com.allwage.clockin.model.employee.Employee;
import com.allwage.clockin.model.Site.GeoCoordinate;
import com.allwage.clockin.model.Site.GeofenceCircle;
import com.allwage.clockin.model.Site.Site;
import com.allwage.clockin.model.Site.SiteAssignment;
import com.allwage.clockin.model.Site.Team;
import com.allwage.clockin.model.Site.ValidationRules;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SiteControllerTest {

    private static final LocalDate ASSIGNMENT_START = LocalDate.of(2026, 1, 1);
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentStore store;

    @BeforeEach
    void setUp() {
        store.clearCollection("sites");
        store.clearCollection("employees");
    }

    @Test
    void createsSite() {
        ResponseEntity<SiteResponse> response = create(
            "/api/sites",
            """
                {
                    "name": "Farm Bravo",
                    "validationRules": null
                }
                """,
            SiteResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).matches(UUID_PATTERN);
        assertThat(response.getBody().name()).isEqualTo("Farm Bravo");
        assertThat(store.findById("sites", response.getBody().id(), Site.class)).contains(
            new Site(response.getBody().id(), "Farm Bravo", null, List.of(), List.of(), List.of())
        );
    }

    @Test
    void addsGeofenceToSite() {
        store.save("sites", "site-1", site());

        ResponseEntity<GeofenceResponse> response = create(
            "/api/sites/site-1/geofences",
            """
                {
                    "latitude": -26.2041,
                    "longitude": 28.0473,
                    "radiusMeters": 100,
                    "primary": true,
                    "effectiveFrom": "2026-02-01",
                    "effectiveTo": null
                }
                """,
            GeofenceResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).matches(UUID_PATTERN);
        assertThat(response.getBody().primary()).isTrue();
        Site savedSite = store.findById("sites", "site-1", Site.class).orElseThrow();
        assertThat(savedSite.geofences()).containsExactly(new GeofenceCircle(
            response.getBody().id(), new GeoCoordinate(-26.2041, 28.0473), 100, true, LocalDate.of(2026, 2, 1), null
        ));
    }

    @Test
    void returnsNotFoundWhenAddingGeofenceToUnknownSite() {
        ResponseEntity<Void> response = create(
            "/api/sites/unknown-site/geofences",
            """
                {
                    "latitude": -26.2041,
                    "longitude": 28.0473,
                    "radiusMeters": 100,
                    "primary": false,
                    "effectiveFrom": "2026-02-01",
                    "effectiveTo": null
                }
                """,
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsInvalidGeofenceRequest() {
        store.save("sites", "site-1", site());

        ResponseEntity<Void> response = create(
            "/api/sites/site-1/geofences",
            """
                {
                    "latitude": 91,
                    "longitude": 28.0473,
                    "radiusMeters": 0,
                    "primary": false,
                    "effectiveFrom": "2026-02-01",
                    "effectiveTo": null
                }
                """,
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(store.findById("sites", "site-1", Site.class).orElseThrow().geofences()).isEmpty();
    }

    @Test
    void rejectsOverlappingPrimaryGeofence() {
        GeofenceCircle existingGeofence = new GeofenceCircle(
            "zone-1", new GeoCoordinate(-26.2041, 28.0473), 100, true, LocalDate.of(2026, 1, 1), null
        );
        store.save("sites", "site-1", site().addGeofence(existingGeofence));

        ResponseEntity<Void> response = create(
            "/api/sites/site-1/geofences",
            """
                {
                    "latitude": -26.2041,
                    "longitude": 28.0473,
                    "radiusMeters": 100,
                    "primary": true,
                    "effectiveFrom": "2026-02-01",
                    "effectiveTo": null
                }
                """,
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(store.findById("sites", "site-1", Site.class).orElseThrow().geofences())
            .containsExactly(existingGeofence);
    }

    @Test
    void rejectsGeofenceWithEndDateBeforeStartDate() {
        store.save("sites", "site-1", site());

        ResponseEntity<Void> response = create(
            "/api/sites/site-1/geofences",
            """
                {
                    "latitude": -26.2041,
                    "longitude": 28.0473,
                    "radiusMeters": 100,
                    "primary": false,
                    "effectiveFrom": "2026-02-02",
                    "effectiveTo": "2026-02-01"
                }
                """,
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(store.findById("sites", "site-1", Site.class).orElseThrow().geofences()).isEmpty();
    }

    @Test
    void addsTeamToSite() {
        store.save("sites", "site-1", site());

        ResponseEntity<Team> response = create(
            "/api/sites/site-1/teams",
            """
                {
                    "name": "Packing",
                    "validationRules": null
                }
                """,
            Team.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).matches(UUID_PATTERN);
        assertThat(response.getBody().name()).isEqualTo("Packing");
        Site savedSite = store.findById("sites", "site-1", Site.class).orElseThrow();
        assertThat(savedSite.teams()).containsExactly(
            new Team("team-1", "Harvest", null),
            response.getBody()
        );
    }

    @Test
    void assignsEmployeeToTeamAtSite() {
        store.save("sites", "site-1", site().addTeam(new Team("team-2", "Packing", null)));
        store.save("employees", "employee-1", new Employee(
            "employee-1", "Nomsa Dlamini", "+27115550123"
        ));

        ResponseEntity<SiteAssignment> response = create(
            "/api/sites/site-1/teams/team-2/assignments",
            """
                {
                    "employeeId": "employee-1",
                    "effectiveFrom": "2026-02-01",
                    "validationRules": null
                }
                """,
            SiteAssignment.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(new SiteAssignment(
            "employee-1", "team-2", LocalDate.of(2026, 2, 1), null, null
        ));
        Site savedSite = store.findById("sites", "site-1", Site.class).orElseThrow();
        assertThat(savedSite.assignments()).containsExactly(
            new SiteAssignment("employee-1", "team-1", ASSIGNMENT_START, LocalDate.of(2026, 1, 31), null),
            new SiteAssignment("employee-1", "team-2", LocalDate.of(2026, 2, 1), null, null)
        );
    }

    @Test
    void doesNotAssignUnknownEmployeeToTeam() {
        store.save("sites", "site-1", site());

        ResponseEntity<Void> response = create(
            "/api/sites/site-1/teams/team-1/assignments",
            """
                {
                    "employeeId": "unknown-employee",
                    "effectiveFrom": "2026-02-01",
                    "validationRules": null
                }
                """,
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Site savedSite = store.findById("sites", "site-1", Site.class).orElseThrow();
        assertThat(savedSite.assignments()).containsExactly(
            new SiteAssignment("employee-1", "team-1", ASSIGNMENT_START, null, null)
        );
    }

    @Test
    void replacesSiteValidationRules() {
        store.save("sites", "site-1", site());

        ResponseEntity<ValidationRules> response = update(
            "/api/sites/site-1/validation-rules",
            """
                {
                    "toleranceMeters": 30,
                    "approvalRequired": true,
                    "strictModeHours": []
                }
                """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ValidationRules(30, true, List.of()));
        Site savedSite = store.findById("sites", "site-1", Site.class).orElseThrow();
        assertThat(savedSite.validationRules()).isEqualTo(new ValidationRules(30, true, List.of()));
    }

    @Test
    void replacesTeamValidationRules() {
        store.save("sites", "site-1", site());

        ResponseEntity<ValidationRules> response = update(
            "/api/sites/site-1/teams/team-1/validation-rules",
            """
                {
                    "toleranceMeters": 10,
                    "approvalRequired": true,
                    "strictModeHours": null
                }
                """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ValidationRules(10, true, null));
        Site savedSite = store.findById("sites", "site-1", Site.class).orElseThrow();
        assertThat(savedSite.teams()).containsExactly(new Team("team-1", "Harvest", new ValidationRules(10, true, null)));
    }

    @Test
    void replacesEmployeeAssignmentValidationRules() {
        store.save("sites", "site-1", site());

        ResponseEntity<ValidationRules> response = update(
            "/api/sites/site-1/employees/employee-1/validation-rules",
            """
                {
                    "effectiveFrom": "2026-01-01",
                    "validationRules": {
                        "toleranceMeters": 50,
                        "approvalRequired": null,
                        "strictModeHours": null
                    }
                }
                """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ValidationRules(50, null, null));
        Site savedSite = store.findById("sites", "site-1", Site.class).orElseThrow();
        assertThat(savedSite.rulesFor("employee-1", ASSIGNMENT_START))
            .hasFieldOrPropertyWithValue("toleranceMeters", 50);
    }

    @Test
    void replacesOnlyTheDatedEmployeeAssignmentInTheRequest() {
        LocalDate replacementStart = LocalDate.of(2026, 2, 1);
        Site site = site()
            .addTeam(new Team("team-2", "Packing", null))
            .assignEmployee("employee-1", "team-2", replacementStart, null);
        store.save("sites", "site-1", site);

        ResponseEntity<ValidationRules> response = update(
            "/api/sites/site-1/employees/employee-1/validation-rules",
            """
                {
                    "effectiveFrom": "2026-02-01",
                    "validationRules": {
                        "toleranceMeters": 50,
                        "approvalRequired": null,
                        "strictModeHours": null
                    }
                }
                """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Site savedSite = store.findById("sites", "site-1", Site.class).orElseThrow();
        assertThat(savedSite.assignments()).containsExactly(
            new SiteAssignment("employee-1", "team-1", ASSIGNMENT_START, LocalDate.of(2026, 1, 31), null),
            new SiteAssignment("employee-1", "team-2", replacementStart, null, new ValidationRules(50, null, null))
        );
    }

    @Test
    void returnsNotFoundWhenTeamDoesNotBelongToSite() {
        store.save("sites", "site-1", site());

        ResponseEntity<ValidationRules> response = update(
            "/api/sites/site-1/teams/unknown-team/validation-rules",
            """
                {
                    "toleranceMeters": 10,
                    "approvalRequired": true,
                    "strictModeHours": null
                }
                """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Site savedSite = store.findById("sites", "site-1", Site.class).orElseThrow();
        assertThat(savedSite.teams()).containsExactly(new Team("team-1", "Harvest", null));
    }

    @Test
    void rejectsNullStrictModeHour() {
        ResponseEntity<ValidationRules> response = update(
            "/api/sites/site-1/validation-rules",
            """
                {
                    "toleranceMeters": 20,
                    "approvalRequired": false,
                    "strictModeHours": [null]
                }
                """
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ValidationRules> update(String path, String requestBody) {
        return restTemplate.exchange(
            path,
            org.springframework.http.HttpMethod.PUT,
            new HttpEntity<>(requestBody, jsonHeaders()),
            ValidationRules.class
        );
    }

    private <T> ResponseEntity<T> create(String path, String requestBody, Class<T> responseType) {
        return restTemplate.postForEntity(path, new HttpEntity<>(requestBody, jsonHeaders()), responseType);
    }

    private Site site() {
        return new Site(
            "site-1",
            "Farm Alpha",
            new ValidationRules(20, false, null),
            List.of(new Team("team-1", "Harvest", null)),
            List.of(),
            List.of()
        ).assignEmployee("employee-1", "team-1", ASSIGNMENT_START, null);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
