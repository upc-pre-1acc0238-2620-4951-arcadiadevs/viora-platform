package com.arcadiadevs.viora.platform.phenology.domain.services;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.DynamicErezPortion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ErezDynamicModelCalculator Domain Service Tests")
class ErezDynamicModelCalculatorTest {

    @Test
    @DisplayName("Should return 0.0 portions when hourly temperature series is empty or null")
    void shouldReturnZeroWhenHourlyTempsEmptyOrNull() {
        assertThat(ErezDynamicModelCalculator.computePortions(null).portions()).isEqualTo(0.0);
        assertThat(ErezDynamicModelCalculator.computePortions(List.of()).portions()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should accumulate portions when temperatures fluctuate in optimal chilling range (4°C - 10°C)")
    void shouldAccumulatePortionsInOptimalRange() {
        // Feed 150 hours of constant 6°C
        List<Double> coolHours = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            coolHours.add(6.0);
        }

        DynamicErezPortion result = ErezDynamicModelCalculator.computePortions(coolHours);
        assertThat(result.portions()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Should degrade intermediate and slow portion accumulation under high heat (> 24°C)")
    void shouldDegradeIntermediateUnderHighHeat() {
        // Series 1: 80 hours at 6°C
        List<Double> coolOnly = Collections.nCopies(80, 6.0);
        DynamicErezPortion coolResult = ErezDynamicModelCalculator.computePortions(coolOnly);

        // Series 2: 40 hours at 6°C, then 10 hours heat wave (28°C), then 40 hours at 6°C
        List<Double> heatInterrupted = new ArrayList<>();
        heatInterrupted.addAll(Collections.nCopies(40, 6.0));
        heatInterrupted.addAll(Collections.nCopies(10, 28.0));
        heatInterrupted.addAll(Collections.nCopies(40, 6.0));

        DynamicErezPortion heatResult = ErezDynamicModelCalculator.computePortions(heatInterrupted);

        // Heat disruption reduces or delays final chilling portion count
        assertThat(heatResult.portions()).isLessThanOrEqualTo(coolResult.portions());
    }

    @Test
    @DisplayName("Should evaluate threshold satisfaction and completion percentage accurately")
    void shouldEvaluateThresholdSatisfactionAndPercentage() {
        double threshold = 27.0;

        assertThat(ErezDynamicModelCalculator.evaluateSatisfactionStatus(28.5, threshold)).isEqualTo("SATISFIED");
        assertThat(ErezDynamicModelCalculator.evaluateSatisfactionStatus(27.0, threshold)).isEqualTo("SATISFIED");
        assertThat(ErezDynamicModelCalculator.evaluateSatisfactionStatus(25.0, threshold)).isEqualTo("DEFICIENT");

        assertThat(ErezDynamicModelCalculator.computeCompletionPercentage(28.5, threshold)).isEqualTo(105.56);
        assertThat(ErezDynamicModelCalculator.computeCompletionPercentage(13.5, threshold)).isEqualTo(50.0);
    }
}
