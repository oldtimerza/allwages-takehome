package com.allwage.clockin.model;

import com.allwage.clockin.model.Site.*;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SiteTest {

    private static final LocalDate FIRST_JANUARY = LocalDate.of(2026, 1, 1);
    private static final ValidationRules SITE_RULES = new ValidationRules(20, false, null);
    private static final Team TEAM = new Team("team-1", "Harvest", new ValidationRules(10, true, null));

    @Test
    void assignsEmployeeToTeamAtThisSite() {
        Site site = site().assignEmployee("employee-1", "team-1", FIRST_JANUARY, null);

        assertThat(site.assignmentFor("employee-1", FIRST_JANUARY))
            .contains(new SiteAssignment("employee-1", "team-1", FIRST_JANUARY, null, null));
    }

    @Test
    void rejectsAssignmentToTeamOutsideSite() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> site().assignEmployee("employee-1", "unknown-team", FIRST_JANUARY, null))
            .withMessage("Team unknown-team does not belong to site site-1");
    }

    @Test
    void reassignmentEndsPreviousTeamAssignmentAndCreatesReplacement() {
        Site site = site()
            .assignEmployee("employee-1", "team-1", FIRST_JANUARY, null)
            .addTeam(new Team("team-2", "Packing", null))
            .assignEmployee("employee-1", "team-2", LocalDate.of(2026, 2, 1), null);

        assertThat(site.assignments()).containsExactly(
            new SiteAssignment("employee-1", "team-1", FIRST_JANUARY, LocalDate.of(2026, 1, 31), null),
            new SiteAssignment("employee-1", "team-2", LocalDate.of(2026, 2, 1), null, null)
        );
    }

    @Test
    void sameDayReassignmentReplacesTheOriginalAssignment() {
        Site site = site()
            .assignEmployee("employee-1", "team-1", FIRST_JANUARY, null)
            .addTeam(new Team("team-2", "Packing", null))
            .assignEmployee("employee-1", "team-2", FIRST_JANUARY, null);

        assertThat(site.assignments()).containsExactly(
            new SiteAssignment("employee-1", "team-2", FIRST_JANUARY, null, null)
        );
    }

    @Test
    void rejectsOverlappingAssignmentsForOneEmployee() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Site(
                "site-1",
                "Farm Alpha",
                SITE_RULES,
                List.of(TEAM),
                List.of(),
                List.of(
                    new SiteAssignment("employee-1", "team-1", FIRST_JANUARY, null, null),
                    new SiteAssignment("employee-1", "team-1", LocalDate.of(2026, 2, 1), null, null)
                )
            ))
            .withMessage("Employee employee-1 has overlapping assignments at site site-1");
    }

    @Test
    void resolvesMostSpecificRulesFromEmployeeAssignment() {
        ValidationRules employeeRules = new ValidationRules(50, null, null);
        Site site = site().assignEmployee("employee-1", "team-1", FIRST_JANUARY, employeeRules);

        assertThat(site.rulesFor("employee-1", FIRST_JANUARY))
            .isEqualTo(new ResolvedValidationRules(50, true, List.of()));
    }

    @Test
    void determinesWhetherGeofenceIsEffectiveOnDate() {
        GeofenceCircle geofence = new GeofenceCircle(
            "zone-1",
            new GeoCoordinate(-26.2041, 28.0473),
            100,
            true,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31)
        );

        assertThat(geofence.isEffectiveOn(LocalDate.of(2026, 1, 31))).isTrue();
        assertThat(geofence.isEffectiveOn(LocalDate.of(2026, 2, 1))).isFalse();
    }

    @Test
    void addsGeofenceToSite() {
        GeofenceCircle geofence = geofence("zone-1", false, FIRST_JANUARY, null);

        Site site = site().addGeofence(geofence);

        assertThat(site.geofences()).containsExactly(geofence);
    }

    @Test
    void rejectsDuplicateGeofenceAtSite() {
        Site site = site().addGeofence(geofence("zone-1", false, FIRST_JANUARY, null));

        assertThatIllegalArgumentException()
            .isThrownBy(() -> site.addGeofence(geofence("zone-1", false, FIRST_JANUARY, null)))
            .withMessage("Geofence zone-1 already belongs to site site-1");
    }

    @Test
    void rejectsOverlappingPrimaryGeofences() {
        GeofenceCircle firstPrimary = geofence("zone-1", true, FIRST_JANUARY, null);
        GeofenceCircle secondPrimary = geofence("zone-2", true, LocalDate.of(2026, 2, 1), null);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> new Site(
                "site-1", "Farm Alpha", SITE_RULES, List.of(TEAM), List.of(firstPrimary, secondPrimary), List.of()
            ))
            .withMessage("Site cannot have overlapping primary geofences");
    }

    @Test
    void resolvesStrictModeHoursFromTheMostSpecificRuleScope() {
        StrictModeHours employeeHours = new StrictModeHours(
            Set.of(DayOfWeek.MONDAY), LocalTime.of(6, 0), LocalTime.of(8, 0), 5
        );
        ValidationRules employeeRules = new ValidationRules(null, null, List.of(employeeHours));
        Site site = site().assignEmployee("employee-1", "team-1", FIRST_JANUARY, employeeRules);

        assertThat(site.rulesFor("employee-1", FIRST_JANUARY).strictModeHours()).containsExactly(employeeHours);
    }

    @Test
    void rejectsNonFiniteCoordinatesAndRadius() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new GeoCoordinate(Double.NaN, 28.0473))
            .withMessage("Latitude must be finite and between -90 and 90");
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new GeofenceCircle(
                "zone-1", new GeoCoordinate(-26.2041, 28.0473), Double.POSITIVE_INFINITY, true, FIRST_JANUARY, null
            ))
            .withMessage("Geofence radius must be finite and positive");
    }

    private Site site() {
        return new Site("site-1", "Farm Alpha", SITE_RULES, List.of(TEAM), List.of(), List.of());
    }

    private GeofenceCircle geofence(String id, boolean primary, LocalDate effectiveFrom, LocalDate effectiveTo) {
        return new GeofenceCircle(
            id, new GeoCoordinate(-26.2041, 28.0473), 100, primary, effectiveFrom, effectiveTo
        );
    }
}
