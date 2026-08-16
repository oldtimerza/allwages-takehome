package com.allwage.clockin.service;

import com.allwage.clockin.model.Site;
import com.allwage.clockin.model.SiteAssignment;
import com.allwage.clockin.model.GeoCoordinate;
import com.allwage.clockin.model.GeofenceCircle;
import com.allwage.clockin.model.Team;
import com.allwage.clockin.model.ValidationRules;
import com.allwage.clockin.repository.employee.EmployeeRepository;
import com.allwage.clockin.repository.site.SiteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class SiteServiceTest {

    private static final String SITE_ID = "site-1";
    private static final String TEAM_ID = "team-1";
    private static final String EMPLOYEE_ID = "employee-1";
    private static final LocalDate FIRST_ASSIGNMENT_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate SECOND_ASSIGNMENT_START = LocalDate.of(2026, 2, 1);
    private static final ValidationRules REPLACEMENT_RULES = new ValidationRules(50, true, List.of());

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Test
    void givenExistingSite_whenReplacingSiteRules_thenReturnsReplacementAndAppliesMutation() {
        // Given
        Site sourceSite = site();
        AtomicReference<Site> updatedSite = new AtomicReference<>();
        SiteService siteService = new SiteService(siteRepository, employeeRepository);
        given(siteRepository.update(eq(SITE_ID), ArgumentMatchers.any()))
            .willAnswer(invocation -> applyMutation(invocation, sourceSite, updatedSite));

        // When
        Optional<ValidationRules> result = siteService.replaceSiteValidationRules(SITE_ID, REPLACEMENT_RULES);

        // Then
        assertThat(result).contains(REPLACEMENT_RULES);
        assertThat(updatedSite.get().validationRules()).isEqualTo(REPLACEMENT_RULES);
        then(siteRepository).should().update(eq(SITE_ID), ArgumentMatchers.any());
        then(siteRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void givenExistingTeam_whenReplacingTeamRules_thenUpdatesOnlyThatTeam() {
        // Given
        Site sourceSite = site();
        AtomicReference<Site> updatedSite = new AtomicReference<>();
        SiteService siteService = new SiteService(siteRepository, employeeRepository);
        given(siteRepository.update(eq(SITE_ID), ArgumentMatchers.any()))
            .willAnswer(invocation -> applyMutation(invocation, sourceSite, updatedSite));

        // When
        Optional<ValidationRules> result = siteService.replaceTeamValidationRules(SITE_ID, TEAM_ID, REPLACEMENT_RULES);

        // Then
        assertThat(result).contains(REPLACEMENT_RULES);
        assertThat(updatedSite.get().teams()).containsExactly(new Team(TEAM_ID, "Harvest", REPLACEMENT_RULES));
        assertThat(updatedSite.get().validationRules()).isEqualTo(new ValidationRules(20, false, null));
        then(siteRepository).should().update(eq(SITE_ID), ArgumentMatchers.any());
        then(siteRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void givenHistoricalAssignments_whenReplacingAssignmentRules_thenUpdatesOnlyTheMatchingDate() {
        // Given
        Site sourceSite = siteWithHistoricalAssignments();
        AtomicReference<Site> updatedSite = new AtomicReference<>();
        SiteService siteService = new SiteService(siteRepository, employeeRepository);
        given(siteRepository.update(eq(SITE_ID), ArgumentMatchers.any()))
            .willAnswer(invocation -> applyMutation(invocation, sourceSite, updatedSite));

        // When
        Optional<ValidationRules> result = siteService.replaceAssignmentValidationRules(
            SITE_ID,
            EMPLOYEE_ID,
            SECOND_ASSIGNMENT_START,
            REPLACEMENT_RULES
        );

        // Then
        assertThat(result).contains(REPLACEMENT_RULES);
        assertThat(updatedSite.get().assignments()).containsExactly(
            new SiteAssignment(
                EMPLOYEE_ID,
                TEAM_ID,
                FIRST_ASSIGNMENT_START,
                LocalDate.of(2026, 1, 31),
                null
            ),
            new SiteAssignment(
                EMPLOYEE_ID,
                "team-2",
                SECOND_ASSIGNMENT_START,
                null,
                REPLACEMENT_RULES
            )
        );
        then(siteRepository).should().update(eq(SITE_ID), ArgumentMatchers.any());
        then(siteRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void givenSiteWithoutTeam_whenReplacingTeamRules_thenReturnsEmptyResult() {
        // Given
        Site sourceSite = site();
        AtomicReference<Site> updatedSite = new AtomicReference<>();
        SiteService siteService = new SiteService(siteRepository, employeeRepository);
        given(siteRepository.update(eq(SITE_ID), ArgumentMatchers.any()))
            .willAnswer(invocation -> applyMutation(invocation, sourceSite, updatedSite));

        // When
        Optional<ValidationRules> result = siteService.replaceTeamValidationRules(
            SITE_ID,
            "unknown-team",
            REPLACEMENT_RULES
        );

        // Then
        assertThat(result).isEmpty();
        assertThat(updatedSite.get()).isNull();
        then(siteRepository).should().update(eq(SITE_ID), ArgumentMatchers.any());
        then(siteRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void givenUnusedSiteId_whenCreatingSite_thenReturnsAndSavesTheSite() {
        // Given
        Site site = site();
        SiteService siteService = new SiteService(siteRepository, employeeRepository);
        given(siteRepository.saveIfAbsent(site)).willReturn(true);

        // When
        Optional<Site> result = siteService.createSite(site);

        // Then
        assertThat(result).contains(site);
        then(siteRepository).should().saveIfAbsent(site);
        then(siteRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void givenExistingSite_whenAddingGeofence_thenReturnsGeofenceAndAppliesMutation() {
        // Given
        Site sourceSite = site();
        GeofenceCircle geofence = new GeofenceCircle(
            "zone-1", new GeoCoordinate(-26.2041, 28.0473), 100, false, FIRST_ASSIGNMENT_START, null
        );
        AtomicReference<Site> updatedSite = new AtomicReference<>();
        SiteService siteService = new SiteService(siteRepository, employeeRepository);
        given(siteRepository.update(eq(SITE_ID), ArgumentMatchers.any()))
            .willAnswer(invocation -> applyMutation(invocation, sourceSite, updatedSite));

        // When
        Optional<GeofenceCircle> result = siteService.addGeofence(SITE_ID, geofence);

        // Then
        assertThat(result).contains(geofence);
        assertThat(updatedSite.get().geofences()).containsExactly(geofence);
        then(siteRepository).should().update(eq(SITE_ID), ArgumentMatchers.any());
        then(siteRepository).shouldHaveNoMoreInteractions();
    }

    private Optional<Site> applyMutation(
        InvocationOnMock invocation,
        Site sourceSite,
        AtomicReference<Site> updatedSite
    ) {
        Function<Site, Optional<Site>> mutation = invocation.getArgument(1);
        Optional<Site> result = mutation.apply(sourceSite);
        result.ifPresent(updatedSite::set);
        return result;
    }

    private Site site() {
        return new Site(
            SITE_ID,
            "Farm Alpha",
            new ValidationRules(20, false, null),
            List.of(new Team(TEAM_ID, "Harvest", null)),
            List.of(),
            List.of()
        );
    }

    private Site siteWithHistoricalAssignments() {
        return new Site(
            SITE_ID,
            "Farm Alpha",
            new ValidationRules(20, false, null),
            List.of(new Team(TEAM_ID, "Harvest", null), new Team("team-2", "Packing", null)),
            List.of(),
            List.of()
        )
            .assignEmployee(EMPLOYEE_ID, TEAM_ID, FIRST_ASSIGNMENT_START, null)
            .assignEmployee(EMPLOYEE_ID, "team-2", SECOND_ASSIGNMENT_START, null);
    }
}
