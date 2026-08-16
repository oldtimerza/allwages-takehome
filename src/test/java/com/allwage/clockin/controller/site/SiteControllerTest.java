package com.allwage.clockin.controller.site;

import com.allwage.clockin.model.Site;
import com.allwage.clockin.model.SiteAssignment;
import com.allwage.clockin.model.Team;
import com.allwage.clockin.model.ValidationRules;
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

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DocumentStore store;

    @BeforeEach
    void setUp() {
        store.clearCollection("sites");
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
