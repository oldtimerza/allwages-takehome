package com.allwage.clockin.service;

import com.allwage.clockin.client.InstantMessagingClient;
import com.allwage.clockin.model.AuditEventType;
import com.allwage.clockin.model.AuditReasonCode;
import com.allwage.clockin.model.AuditSource;
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
import com.allwage.clockin.repository.employee.EmployeeRepository;
import com.allwage.clockin.repository.site.SiteRepository;
import com.allwage.clockin.repository.store.DocumentStore;
import com.allwage.clockin.service.audit.AuditDraft;
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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class ClockServiceTest {

    private static final String EMPLOYEE_ID = "employee-1";
    private static final String SITE_ID = "site-1";
    private static final String PRIMARY_GEOFENCE_ID = "primary-zone";
    private static final LocalDate CLOCK_DATE = LocalDate.of(2026, 1, 5);

    @Mock
    private DocumentStore store;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private InstantMessagingClient instantMessagingClient;

    @Mock
    private AuditWriter auditWriter;

    @Mock
    private ClockProcessingAuditMapper auditMapper;

    @Test
    void givenClockInsideAssignedPrimaryGeofence_whenProcessing_thenAcceptsPersistsAndNotifiesEmployee() {
        // Given
        ClockService clockService = clockService();
        Employee employee = employee();
        Site site = site(SITE_ID, true, false);
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(siteRepository.findAssignedTo(EMPLOYEE_ID, CLOCK_DATE)).willReturn(List.of(site));
        given(instantMessagingClient.sendMessage(employee.phoneNumber(), "Your clock-in was accepted.")).willReturn(true);

        // When
        ValidatedClockEvent result = clockService.processClock(clockEvent());

        // Then
        assertThat(result.validationResult()).isEqualTo(new ClockValidationResult(
            ClockValidationResult.Decision.ACCEPTED,
            ClockValidationResult.Reason.ACCEPTED,
            SITE_ID,
            PRIMARY_GEOFENCE_ID
        ));
        then(store).should().save("clocks", "clock-1", result);
        then(instantMessagingClient).should().sendMessage(employee.phoneNumber(), "Your clock-in was accepted.");
    }

    @Test
    void givenClockOutsideAllAssignedGeofences_whenProcessing_thenPersistsRejectedResultAndNotifiesEmployee() {
        // Given
        ClockService clockService = clockService();
        Employee employee = employee();
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(siteRepository.findAssignedTo(EMPLOYEE_ID, CLOCK_DATE)).willReturn(List.of(site(SITE_ID, true, false)));
        given(instantMessagingClient.sendMessage(employee.phoneNumber(), "Your clock-in requires attention.")).willReturn(true);

        // When
        ValidatedClockEvent result = clockService.processClock(clockEventAt(-26.2141, 28.0473));

        // Then
        assertThat(result.validationResult()).isEqualTo(new ClockValidationResult(
            ClockValidationResult.Decision.REJECTED,
            ClockValidationResult.Reason.OUTSIDE_GEOFENCE,
            null,
            null
        ));
        then(store).should().save("clocks", "clock-1", result);
        then(instantMessagingClient).should().sendMessage(employee.phoneNumber(), "Your clock-in requires attention.");
    }

    @Test
    void givenUnknownEmployee_whenProcessing_thenPersistsEmployeeNotFoundResultWithoutLookingUpSites() {
        // Given
        ClockService clockService = clockService();
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.empty());

        // When
        ValidatedClockEvent result = clockService.processClock(clockEvent());

        // Then
        assertThat(result.validationResult().reason()).isEqualTo(ClockValidationResult.Reason.EMPLOYEE_NOT_FOUND);
        then(siteRepository).shouldHaveNoInteractions();
        then(instantMessagingClient).shouldHaveNoInteractions();
    }

    @Test
    void givenFutureClockTimestamp_whenProcessing_thenPersistsFutureTimestampResultWithoutLookingUpSites() {
        // Given
        ClockService clockService = clockService();

        // When
        ValidatedClockEvent result = clockService.processClock(clockEventAtTime("2100-01-01T07:00:00+02:00"));

        // Then
        assertThat(result.validationResult().reason()).isEqualTo(ClockValidationResult.Reason.FUTURE_TIMESTAMP);
        then(siteRepository).shouldHaveNoInteractions();
        then(instantMessagingClient).shouldHaveNoInteractions();
    }

    @Test
    void givenEmployeeWithoutAnActiveSiteAssignment_whenProcessing_thenPersistsNoAssignmentResult() {
        // Given
        ClockService clockService = clockService();
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee()));
        given(siteRepository.findAssignedTo(EMPLOYEE_ID, CLOCK_DATE)).willReturn(List.of());

        // When
        ValidatedClockEvent result = clockService.processClock(clockEvent());

        // Then
        assertThat(result.validationResult().reason()).isEqualTo(ClockValidationResult.Reason.NO_SITE_ASSIGNMENT);
        then(instantMessagingClient).should().sendMessage(employee().phoneNumber(), "Your clock-in requires attention.");
    }

    @Test
    void givenMatchingGeofencesForDifferentAssignedSites_whenProcessing_thenRejectsAsAmbiguous() {
        // Given
        ClockService clockService = clockService();
        Employee employee = employee();
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(siteRepository.findAssignedTo(EMPLOYEE_ID, CLOCK_DATE)).willReturn(List.of(
            site(SITE_ID, true, false),
            site("site-2", true, false)
        ));
        given(instantMessagingClient.sendMessage(employee.phoneNumber(), "Your clock-in requires attention.")).willReturn(true);

        // When
        ValidatedClockEvent result = clockService.processClock(clockEvent());

        // Then
        assertThat(result.validationResult().reason()).isEqualTo(ClockValidationResult.Reason.AMBIGUOUS_SITE);
        then(store).should().save("clocks", "clock-1", result);
    }

    @Test
    void givenNonPrimaryGeofenceRequiresApproval_whenProcessing_thenPersistsAttentionResult() {
        // Given
        ClockService clockService = clockService();
        Employee employee = employee();
        Site site = site(SITE_ID, false, true);
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(siteRepository.findAssignedTo(EMPLOYEE_ID, CLOCK_DATE)).willReturn(List.of(site));
        given(instantMessagingClient.sendMessage(employee.phoneNumber(), "Your clock-in requires attention.")).willReturn(true);

        // When
        ValidatedClockEvent result = clockService.processClock(clockEvent());

        // Then
        assertThat(result.validationResult()).isEqualTo(new ClockValidationResult(
            ClockValidationResult.Decision.REJECTED,
            ClockValidationResult.Reason.APPROVAL_REQUIRED,
            SITE_ID,
            PRIMARY_GEOFENCE_ID
        ));
        then(store).should().save("clocks", "clock-1", result);
    }

    @Test
    void givenNotificationFailure_whenProcessing_thenRetainsPersistedAcceptedResult() {
        // Given
        ClockService clockService = clockService();
        Employee employee = employee();
        given(employeeRepository.findById(EMPLOYEE_ID)).willReturn(Optional.of(employee));
        given(siteRepository.findAssignedTo(EMPLOYEE_ID, CLOCK_DATE)).willReturn(List.of(site(SITE_ID, true, false)));
        given(instantMessagingClient.sendMessage(employee.phoneNumber(), "Your clock-in was accepted.")).willReturn(false);

        // When
        ValidatedClockEvent result = clockService.processClock(clockEvent());

        // Then
        assertThat(result.validationResult().decision()).isEqualTo(ClockValidationResult.Decision.ACCEPTED);
        then(store).should().save("clocks", "clock-1", result);
        then(instantMessagingClient).should().sendMessage(employee.phoneNumber(), "Your clock-in was accepted.");
    }

    private ClockService clockService() {
        givenAtomicStore();
        given(auditMapper.auditFor(any(ValidatedClockEvent.class))).willAnswer(invocation -> auditDraft(invocation.getArgument(0)));
        return new ClockService(store, siteRepository, employeeRepository, instantMessagingClient, auditWriter, auditMapper);
    }

    private void givenAtomicStore() {
        given(store.executeAtomically(any())).willAnswer(invocation -> {
            Supplier<?> operation = invocation.getArgument(0);
            return operation.get();
        });
    }

    private AuditDraft auditDraft(ValidatedClockEvent clockEvent) {
        return new AuditDraft(
            clockEvent.clockEvent().id(),
            clockEvent.clockEvent().employeeId(),
            AuditEventType.CLOCK_ACCEPTED,
            new ClockAuditPayload(AuditReasonCode.CLOCK_ACCEPTED, AuditSource.MOBILE_API, 200)
        );
    }

    private Employee employee() {
        return new Employee(EMPLOYEE_ID, "Employee One", "+27115550100");
    }

    private ClockEvent clockEvent() {
        return clockEventAt(-26.2041, 28.0473);
    }

    private ClockEvent clockEventAt(double latitude, double longitude) {
        return clockEventAt(latitude, longitude, "2026-01-05T07:00:00+02:00");
    }

    private ClockEvent clockEventAtTime(String timestamp) {
        return clockEventAt(-26.2041, 28.0473, timestamp);
    }

    private ClockEvent clockEventAt(double latitude, double longitude, String timestamp) {
        return new ClockEvent(
            "clock-1",
            EMPLOYEE_ID,
            ZonedDateTime.parse(timestamp),
            latitude,
            longitude,
            10,
            ClockEvent.ClockType.IN
        );
    }

    private Site site(String siteId, boolean primary, boolean approvalRequired) {
        return new Site(
            siteId,
            "Farm Alpha",
            new ValidationRules(20, approvalRequired, null),
            List.of(new Team("team-1", "Harvest", null)),
            List.of(new GeofenceCircle(
                PRIMARY_GEOFENCE_ID,
                new GeoCoordinate(-26.2041, 28.0473),
                100,
                primary,
                LocalDate.of(2026, 1, 1),
                null
            )),
            List.of(new SiteAssignment(EMPLOYEE_ID, "team-1", LocalDate.of(2026, 1, 1), null, null))
        );
    }
}
