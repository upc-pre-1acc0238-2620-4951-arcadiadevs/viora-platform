package com.arcadiadevs.viora.platform.phenology.domain.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Reference values were computed with an independent implementation of the Dynamic Model spreadsheet
 * (the same equations as {@code chillR::Dynamic_Model}).
 */
@DisplayName("ErezDynamicModelCalculator Domain Service Tests")
class ErezDynamicModelCalculatorTest {

    @Test
    @DisplayName("Should return 0.0 portions when hourly temperature series is empty or null")
    void shouldReturnZeroWhenHourlyTempsEmptyOrNull() {
        assertThat(ErezDynamicModelCalculator.computePortions(null).portions()).isEqualTo(0.0);
        assertThat(ErezDynamicModelCalculator.computePortions(List.of()).portions()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should fix about 0.81 portions a day at a constant 6 °C, the optimum of the model")
    void shouldMatchTheReferenceAtTheOptimum() {
        var portions = ErezDynamicModelCalculator.computePortions(Collections.nCopies(30 * 24, 6.0));

        assertThat(portions.portions()).isCloseTo(24.43, offset(0.01));
    }

    @Test
    @DisplayName("Should accumulate less at 2 °C than at 6 °C and nothing at a constant 15 °C")
    void shouldMatchTheReferenceOutsideTheOptimum() {
        assertThat(ErezDynamicModelCalculator.computePortions(Collections.nCopies(10 * 24, 2.0)).portions())
                .isCloseTo(5.71, offset(0.01));
        assertThat(ErezDynamicModelCalculator.computePortions(Collections.nCopies(10 * 24, 15.0)).portions())
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should need 29 cool hours before the first portion is fixed")
    void shouldBuildTheIntermediateBeforeFixingAPortion() {
        double[] cumulative = ErezDynamicModelCalculator.cumulativePortions(Collections.nCopies(40, 6.0));

        assertThat(cumulative).hasSize(40);
        assertThat(cumulative[27]).isEqualTo(0.0);
        assertThat(cumulative[28]).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should lose the intermediate built before a warm interruption")
    void shouldDestroyTheIntermediateUnderHeat() {
        List<Double> heatInterrupted = new ArrayList<>();
        heatInterrupted.addAll(Collections.nCopies(40, 6.0));
        heatInterrupted.addAll(Collections.nCopies(10, 28.0));
        heatInterrupted.addAll(Collections.nCopies(40, 6.0));

        var heat = ErezDynamicModelCalculator.computePortions(heatInterrupted).portions();
        var cool = ErezDynamicModelCalculator.computePortions(Collections.nCopies(90, 6.0)).portions();

        assertThat(heat).isCloseTo(1.96, offset(0.01));
        assertThat(cool).isCloseTo(2.93, offset(0.01));
    }

    @Test
    @DisplayName("Should keep the running total on hours without a value")
    void shouldSkipMissingHours() {
        List<Double> withGap = new ArrayList<>(Collections.nCopies(30, 6.0));
        withGap.add(null);

        double[] cumulative = ErezDynamicModelCalculator.cumulativePortions(withGap);

        assertThat(cumulative[30]).isEqualTo(cumulative[29]);
    }

    @Test
    @DisplayName("Should evaluate threshold satisfaction and completion percentage against 30 portions")
    void shouldEvaluateThresholdSatisfactionAndPercentage() {
        double threshold = ErezDynamicModelCalculator.DEFAULT_VARIETAL_CHILL_THRESHOLD;

        assertThat(threshold).isEqualTo(30.0);
        assertThat(ErezDynamicModelCalculator.evaluateSatisfactionStatus(30.0, threshold)).isEqualTo("SATISFIED");
        assertThat(ErezDynamicModelCalculator.evaluateSatisfactionStatus(29.9, threshold)).isEqualTo("DEFICIENT");
        assertThat(ErezDynamicModelCalculator.computeCompletionPercentage(15.0, threshold)).isEqualTo(50.0);
        assertThat(ErezDynamicModelCalculator.computeCompletionPercentage(3.0, threshold)).isEqualTo(10.0);
    }
}
