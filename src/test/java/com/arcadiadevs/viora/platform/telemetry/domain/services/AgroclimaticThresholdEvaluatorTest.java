package com.arcadiadevs.viora.platform.telemetry.domain.services;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.SoilTextureType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgroclimaticThresholdEvaluator Domain Service Unit Tests")
class AgroclimaticThresholdEvaluatorTest {

    private final AgroclimaticThresholdEvaluator evaluator = new AgroclimaticThresholdEvaluator();

    @Test
    @DisplayName("Should detect critical hydric stress for sandy soil when moisture is below 12%")
    void shouldDetectCriticalHydricStressForSandySoil() {
        var triggerOpt = evaluator.evaluateHydricRisk(11.0, SoilTextureType.SANDY);
        assertThat(triggerOpt).isPresent();
        var trigger = triggerOpt.get();
        assertThat(trigger.type()).isEqualTo(IncidentType.HYDRIC_STRESS);
        assertThat(trigger.severity()).isEqualTo(IncidentSeverity.CRITICAL);
        assertThat(trigger.breachInfo().currentValue()).isEqualTo(11.0);
        assertThat(trigger.breachInfo().thresholdValue()).isEqualTo(12.0);
        assertThat(trigger.mitigationInstructionKeys()).isNotEmpty();
    }

    @Test
    @DisplayName("Should detect warning hydric stress for loam soil when moisture is between 16% and 20%")
    void shouldDetectWarningHydricStressForLoamSoil() {
        var triggerOpt = evaluator.evaluateHydricRisk(18.5, SoilTextureType.LOAM);
        assertThat(triggerOpt).isPresent();
        var trigger = triggerOpt.get();
        assertThat(trigger.type()).isEqualTo(IncidentType.HYDRIC_STRESS);
        assertThat(trigger.severity()).isEqualTo(IncidentSeverity.WARNING);
        assertThat(trigger.breachInfo().thresholdValue()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Should return empty when soil moisture is optimal")
    void shouldReturnEmptyWhenMoistureOptimal() {
        var triggerOpt = evaluator.evaluateHydricRisk(25.0, SoilTextureType.LOAM);
        assertThat(triggerOpt).isEmpty();
    }

    @Test
    @DisplayName("Should detect heat wave when temperature >= 36°C as critical")
    void shouldDetectCriticalHeatWave() {
        var triggerOpt = evaluator.evaluateThermalRisk(37.5, 30.0);
        assertThat(triggerOpt).isPresent();
        var trigger = triggerOpt.get();
        assertThat(trigger.type()).isEqualTo(IncidentType.HEAT_WAVE);
        assertThat(trigger.severity()).isEqualTo(IncidentSeverity.CRITICAL);
        assertThat(trigger.breachInfo().currentValue()).isEqualTo(37.5);
    }

    @Test
    @DisplayName("Should detect heat wave when temperature is between 32°C and 36°C as warning")
    void shouldDetectWarningHeatWave() {
        var triggerOpt = evaluator.evaluateThermalRisk(33.0, 35.0);
        assertThat(triggerOpt).isPresent();
        var trigger = triggerOpt.get();
        assertThat(trigger.type()).isEqualTo(IncidentType.HEAT_WAVE);
        assertThat(trigger.severity()).isEqualTo(IncidentSeverity.WARNING);
    }

    @Test
    @DisplayName("Should detect critical frost when minimum temperature is <= -2.0°C")
    void shouldDetectCriticalFrost() {
        var triggerOpt = evaluator.evaluateFrostRisk(-3.0);
        assertThat(triggerOpt).isPresent();
        var trigger = triggerOpt.get();
        assertThat(trigger.type()).isEqualTo(IncidentType.FROST_WARNING);
        assertThat(trigger.severity()).isEqualTo(IncidentSeverity.CRITICAL);
    }

    @Test
    @DisplayName("Should detect warning frost when minimum temperature is between -2.0°C and 1.0°C")
    void shouldDetectWarningFrost() {
        var triggerOpt = evaluator.evaluateFrostRisk(0.5);
        assertThat(triggerOpt).isPresent();
        var trigger = triggerOpt.get();
        assertThat(trigger.type()).isEqualTo(IncidentType.FROST_WARNING);
        assertThat(trigger.severity()).isEqualTo(IncidentSeverity.WARNING);
    }

    @Test
    @DisplayName("Should return empty when frost risk is not present")
    void shouldReturnEmptyWhenNoFrost() {
        var triggerOpt = evaluator.evaluateFrostRisk(5.0);
        assertThat(triggerOpt).isEmpty();
    }
}
