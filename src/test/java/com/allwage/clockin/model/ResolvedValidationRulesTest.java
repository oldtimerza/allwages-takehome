package com.allwage.clockin.model;

import com.allwage.clockin.model.Site.ResolvedValidationRules;
import com.allwage.clockin.model.Site.StrictModeHours;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResolvedValidationRulesTest {

    @Test
    void usesTheTightestStrictToleranceDuringAnActiveSastWindow() {
        ResolvedValidationRules rules = new ResolvedValidationRules(
            30,
            false,
            List.of(new StrictModeHours(
                Set.of(DayOfWeek.MONDAY),
                LocalTime.of(6, 0),
                LocalTime.of(8, 0),
                10
            ))
        );

        int tolerance = rules.toleranceAt(ZonedDateTime.parse("2026-01-05T07:00:00+02:00"));

        assertThat(tolerance).isEqualTo(10);
    }
}
