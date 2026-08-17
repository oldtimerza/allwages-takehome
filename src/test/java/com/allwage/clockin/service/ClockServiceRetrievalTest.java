package com.allwage.clockin.service;

import com.allwage.clockin.client.InstantMessagingClient;
import com.allwage.clockin.model.clock.ClockEvent;
import com.allwage.clockin.model.clock.ClockPage;
import com.allwage.clockin.model.clock.ClockPageQuery;
import com.allwage.clockin.model.clock.ClockValidationResult;
import com.allwage.clockin.model.employee.Employee;
import com.allwage.clockin.model.Site.GeoCoordinate;
import com.allwage.clockin.model.Site.GeofenceCircle;
import com.allwage.clockin.model.Site.Site;
import com.allwage.clockin.model.Site.SiteAssignment;
import com.allwage.clockin.model.Site.Team;
import com.allwage.clockin.model.Site.ValidatedClockEvent;
import com.allwage.clockin.repository.clock.ClockEventRepository;
import com.allwage.clockin.repository.employee.EmployeeRepository;
import com.allwage.clockin.repository.site.SiteRepository;
import com.allwage.clockin.repository.transaction.TransactionRepository;
import com.allwage.clockin.service.audit.AuditWriter;
import com.allwage.clockin.service.audit.ClockProcessingAuditMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ClockServiceRetrievalTest {

    private static final String EMPLOYEE_ID = "employee-1";
    private static final String SITE_ID = "site-1";
    private static final String TEAM_ID = "team-1";

    @Mock
    private ClockEventRepository clockEventRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InstantMessagingClient instantMessagingClient;

    @Mock
    private AuditWriter auditWriter;

    @Mock
    private ClockProcessingAuditMapper auditMapper;

    @Test
    void givenRejectedClockOnHistoricalTeam_whenFindingTeamPage_thenIncludesClockInNewestFirstOrder() {
        // Given
        ClockService clockService = clockService();
        given(siteRepository.findById(SITE_ID)).willReturn(Optional.of(site()));
        given(clockEventRepository.findAll()).willReturn(List.of(
            clock("clock-older", "2026-08-16T09:00:00+02:00"),
            clock("clock-newer", "2026-08-16T10:00:00+02:00")
        ));

        // When
        Optional<ClockPage> result = clockService.findPageForTeam(SITE_ID, TEAM_ID, new ClockPageQuery(0, 50, null));

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().entries()).extracting(clock -> clock.clockEvent().id())
            .containsExactly("clock-newer", "clock-older");
        assertThat(result.orElseThrow().totalElements()).isEqualTo(2);
        then(siteRepository).should().findById(SITE_ID);
        then(clockEventRepository).should().findAll();
    }

    @Test
    void givenClockAtUtcDateBoundary_whenFindingTeamPage_thenUsesSastAssignmentDate() {
        // Given
        ClockService clockService = clockService();
        given(siteRepository.findById(SITE_ID)).willReturn(Optional.of(siteWithTeamChange()));
        given(clockEventRepository.findAll()).willReturn(List.of(
            clock("clock-boundary", "2026-08-16T23:30:00Z")
        ));

        // When
        Optional<ClockPage> historicalTeamPage = clockService.findPageForTeam(
            SITE_ID,
            TEAM_ID,
            new ClockPageQuery(0, 50, null)
        );
        Optional<ClockPage> currentTeamPage = clockService.findPageForTeam(
            SITE_ID,
            "team-2",
            new ClockPageQuery(0, 50, null)
        );

        // Then
        assertThat(historicalTeamPage).isPresent();
        assertThat(historicalTeamPage.orElseThrow().entries()).isEmpty();
        assertThat(currentTeamPage).isPresent();
        assertThat(currentTeamPage.orElseThrow().entries()).hasSize(1);
    }

    @Test
    void givenEqualTimestamps_whenFindingEmployeePage_thenUsesIdAsDescendingTieBreaker() {
        // Given
        ClockService clockService = clockService();
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(new Employee(
            EMPLOYEE_ID,
            "Employee One",
            "+27115550100"
        )));
        given(clockEventRepository.findAll()).willReturn(List.of(
            clock("clock-a", "2026-08-16T09:00:00+02:00"),
            clock("clock-b", "2026-08-16T09:00:00+02:00")
        ));

        // When
        Optional<ClockPage> result = clockService.findPageForEmployee(EMPLOYEE_ID, new ClockPageQuery(0, 50, null));

        // Then
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().entries()).extracting(clock -> clock.clockEvent().id())
            .containsExactly("clock-b", "clock-a");
    }

    private ClockService clockService() {
        return new ClockService(
            clockEventRepository,
            siteRepository,
            employeeRepository,
            transactionRepository,
            instantMessagingClient,
            auditWriter,
            auditMapper
        );
    }

    private Site site() {
        return new Site(
            SITE_ID,
            "Site One",
            null,
            List.of(new Team(TEAM_ID, "Team One", null)),
            List.of(new GeofenceCircle(
                "geofence-1",
                new GeoCoordinate(-26.2041, 28.0473),
                100,
                true,
                LocalDate.of(2026, 1, 1),
                null
            )),
            List.of(new SiteAssignment(EMPLOYEE_ID, TEAM_ID, LocalDate.of(2026, 1, 1), null, null))
        );
    }

    private Site siteWithTeamChange() {
        return new Site(
            SITE_ID,
            "Site One",
            null,
            List.of(new Team(TEAM_ID, "Team One", null), new Team("team-2", "Team Two", null)),
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
                    TEAM_ID,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 8, 16),
                    null
                ),
                new SiteAssignment(EMPLOYEE_ID, "team-2", LocalDate.of(2026, 8, 17), null, null)
            )
        );
    }

    private ValidatedClockEvent clock(String id, String timestamp) {
        return new ValidatedClockEvent(
            new ClockEvent(
                id,
                EMPLOYEE_ID,
                ZonedDateTime.parse(timestamp),
                -26.2041,
                28.0473,
                10,
                ClockEvent.ClockType.IN
            ),
            new ClockValidationResult(
                ClockValidationResult.Decision.REJECTED,
                ClockValidationResult.Reason.OUTSIDE_GEOFENCE,
                null,
                null
            )
        );
    }
}
