package com.arcadiadevs.viora.platform.phenology.domain.services;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillProjectionStatus;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillSeasonState;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HourlyTemperature;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ThermalAnomalyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

@DisplayName("ChillSeasonEvaluator Domain Service Tests")
class ChillSeasonEvaluatorTest {

    private static final double THRESHOLD = 30.0;
    private static final double COLD = 6.0;
    private static final double WARM = 26.0;
    private static final double MILD = 15.0;

    @Test
    @DisplayName("Should pick the current winter from June 1 on and the last one before")
    void shouldResolveTheSeasonYear() {
        assertThat(ChillSeasonEvaluator.seasonYearFor(LocalDate.of(2026, 5, 31))).isEqualTo(2025);
        assertThat(ChillSeasonEvaluator.seasonYearFor(LocalDate.of(2026, 6, 1))).isEqualTo(2026);
        assertThat(ChillSeasonEvaluator.seasonYearFor(LocalDate.of(2026, 10, 7))).isEqualTo(2026);
    }

    @Test
    @DisplayName("Should accumulate a season in progress and project the date from the pace of the last 14 days")
    void shouldProjectASeasonInProgress() {
        var hours = days(LocalDate.of(2026, 6, 1), 30, COLD);

        var evaluation = ChillSeasonEvaluator.evaluate(2026, hours, THRESHOLD, LocalDate.of(2026, 7, 1));

        assertThat(evaluation.seasonState()).isEqualTo(ChillSeasonState.IN_PROGRESS);
        assertThat(evaluation.evaluatedThrough()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(evaluation.accumulatedPortions()).isCloseTo(24.43, offset(0.01));
        assertThat(evaluation.completionDate()).isNull();
        assertThat(evaluation.dailyCurve()).hasSize(30);
        // About 0.81 portions a day; the 5.57 still missing take 7 more days.
        assertThat(evaluation.projectionStatus()).isEqualTo(ChillProjectionStatus.PROJECTED);
        assertThat(evaluation.projectedCompletionDate()).isEqualTo(LocalDate.of(2026, 7, 7));
        assertThat(evaluation.thermalAnomaly()).isEqualTo(ThermalAnomalyStatus.NONE);
        assertThat(evaluation.idleDays()).isZero();
    }

    @Test
    @DisplayName("Should mark the first day the threshold is reached and stop projecting")
    void shouldCompleteTheSeason() {
        var hours = days(LocalDate.of(2026, 6, 1), 44, COLD);

        var evaluation = ChillSeasonEvaluator.evaluate(2026, hours, THRESHOLD, LocalDate.of(2026, 7, 15));

        assertThat(evaluation.seasonState()).isEqualTo(ChillSeasonState.COMPLETED);
        assertThat(evaluation.completionDate()).isEqualTo(LocalDate.of(2026, 7, 7));
        assertThat(evaluation.portionsOn(LocalDate.of(2026, 7, 6))).isLessThan(THRESHOLD);
        assertThat(evaluation.portionsOn(LocalDate.of(2026, 7, 7))).isGreaterThanOrEqualTo(THRESHOLD);
        assertThat(evaluation.projectionStatus()).isEqualTo(ChillProjectionStatus.NOT_APPLICABLE);
        assertThat(evaluation.projectedCompletionDate()).isNull();
    }

    @Test
    @DisplayName("Should halt the season while more than 3 warm days in a row are going on")
    void shouldHaltDuringAWarmSpell() {
        var hours = new ArrayList<HourlyTemperature>();
        hours.addAll(days(LocalDate.of(2026, 6, 1), 15, COLD));
        hours.addAll(days(LocalDate.of(2026, 6, 16), 5, WARM));

        var evaluation = ChillSeasonEvaluator.evaluate(2026, hours, THRESHOLD, LocalDate.of(2026, 6, 21));

        assertThat(evaluation.seasonState()).isEqualTo(ChillSeasonState.HALTED);
        assertThat(evaluation.thermalAnomaly()).isEqualTo(ThermalAnomalyStatus.ACTIVE);
        assertThat(evaluation.currentWarmStreakDays()).isEqualTo(5);
        assertThat(evaluation.longestWarmStreakDays()).isEqualTo(5);
        assertThat(evaluation.daysAbove24Celsius()).isEqualTo(5);
        assertThat(evaluation.idleDays()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should not raise the anomaly for exactly 3 warm days in a row")
    void shouldIgnoreThreeWarmDays() {
        var hours = new ArrayList<HourlyTemperature>();
        hours.addAll(days(LocalDate.of(2026, 6, 1), 15, COLD));
        hours.addAll(days(LocalDate.of(2026, 6, 16), 3, WARM));

        var evaluation = ChillSeasonEvaluator.evaluate(2026, hours, THRESHOLD, LocalDate.of(2026, 6, 19));

        assertThat(evaluation.thermalAnomaly()).isEqualTo(ThermalAnomalyStatus.NONE);
        assertThat(evaluation.seasonState()).isEqualTo(ChillSeasonState.IN_PROGRESS);
        assertThat(evaluation.currentWarmStreakDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should keep a warm spell that already ended as recorded")
    void shouldRecordAPastWarmSpell() {
        var hours = new ArrayList<HourlyTemperature>();
        hours.addAll(days(LocalDate.of(2026, 6, 1), 4, WARM));
        hours.addAll(days(LocalDate.of(2026, 6, 5), 20, COLD));

        var evaluation = ChillSeasonEvaluator.evaluate(2026, hours, THRESHOLD, LocalDate.of(2026, 6, 25));

        assertThat(evaluation.thermalAnomaly()).isEqualTo(ThermalAnomalyStatus.RECORDED);
        assertThat(evaluation.seasonState()).isEqualTo(ChillSeasonState.IN_PROGRESS);
        assertThat(evaluation.currentWarmStreakDays()).isZero();
        assertThat(evaluation.longestWarmStreakDays()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should say the threshold is not reachable when the season stopped accumulating")
    void shouldNotProjectWithoutPace() {
        var hours = days(LocalDate.of(2026, 6, 1), 61, MILD);

        var evaluation = ChillSeasonEvaluator.evaluate(2026, hours, THRESHOLD, LocalDate.of(2026, 8, 1));

        assertThat(evaluation.accumulatedPortions()).isZero();
        assertThat(evaluation.projectionStatus()).isEqualTo(ChillProjectionStatus.NOT_REACHABLE_IN_SEASON);
        assertThat(evaluation.projectedCompletionDate()).isNull();
        assertThat(evaluation.idleDays()).isEqualTo(61);
    }

    @Test
    @DisplayName("Should not project with fewer than 14 evaluated days")
    void shouldNeedTwoWeeksToProject() {
        var hours = days(LocalDate.of(2026, 6, 1), 9, COLD);

        var evaluation = ChillSeasonEvaluator.evaluate(2026, hours, THRESHOLD, LocalDate.of(2026, 6, 10));

        assertThat(evaluation.projectionStatus()).isEqualTo(ChillProjectionStatus.INSUFFICIENT_DATA);
        assertThat(evaluation.projectedCompletionDate()).isNull();
    }

    @Test
    @DisplayName("Should describe the finished winter outside the season")
    void shouldCloseTheSeasonOutsideTheWindow() {
        var hours = days(LocalDate.of(2026, 6, 1), 92, MILD);

        var evaluation = ChillSeasonEvaluator.evaluate(2026, hours, THRESHOLD, LocalDate.of(2026, 10, 7));

        assertThat(evaluation.seasonState()).isEqualTo(ChillSeasonState.OFF_SEASON);
        assertThat(evaluation.evaluatedThrough()).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(evaluation.dailyCurve()).hasSize(92);
        assertThat(evaluation.projectionStatus()).isEqualTo(ChillProjectionStatus.NOT_APPLICABLE);
    }

    @Test
    @DisplayName("Should ignore the hours of today and the hours outside June 1 - August 31")
    void shouldOnlyEvaluateWholePastDaysOfTheSeason() {
        var hours = new ArrayList<HourlyTemperature>();
        hours.addAll(days(LocalDate.of(2026, 5, 25), 7, COLD));
        hours.addAll(days(LocalDate.of(2026, 6, 1), 3, COLD));
        hours.addAll(days(LocalDate.of(2026, 6, 4), 1, WARM));

        var evaluation = ChillSeasonEvaluator.evaluate(2026, hours, THRESHOLD, LocalDate.of(2026, 6, 4));

        assertThat(evaluation.evaluatedThrough()).isEqualTo(LocalDate.of(2026, 6, 3));
        assertThat(evaluation.dailyCurve()).hasSize(3);
        assertThat(evaluation.daysAbove24Celsius()).isZero();
        assertThat(evaluation.accumulatedPortions())
                .isEqualTo(ErezDynamicModelCalculator.computePortions(java.util.Collections.nCopies(72, COLD)).portions());
    }

    @Test
    @DisplayName("Should return an empty evaluation on June 1, before any whole day exists")
    void shouldStartEmpty() {
        var evaluation = ChillSeasonEvaluator.evaluate(2026, List.of(), THRESHOLD, LocalDate.of(2026, 6, 1));

        assertThat(evaluation.seasonState()).isEqualTo(ChillSeasonState.IN_PROGRESS);
        assertThat(evaluation.evaluatedThrough()).isNull();
        assertThat(evaluation.accumulatedPortions()).isZero();
        assertThat(evaluation.projectionStatus()).isEqualTo(ChillProjectionStatus.INSUFFICIENT_DATA);
    }

    private static List<HourlyTemperature> days(LocalDate from, int count, double celsius) {
        var hours = new ArrayList<HourlyTemperature>();
        for (int day = 0; day < count; day++) {
            for (int hour = 0; hour < 24; hour++) {
                hours.add(new HourlyTemperature(from.plusDays(day).atTime(hour, 0), celsius));
            }
        }
        return hours;
    }
}
