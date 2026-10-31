package com.arcadiadevs.viora.platform.telemetry.domain.services;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.SoilTextureType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.ThresholdBreachInfo;

import java.util.List;
import java.util.Optional;

/**
 * Pure domain service evaluating agroclimatic and soil threshold limits.
 *
 * <p>Free of framework dependencies. Returns an {@link IncidentTrigger} if thresholds
 * are violated, or empty if conditions remain within normal operating ranges.</p>
 */
public class AgroclimaticThresholdEvaluator {

    private static final List<String> HYDRIC_MITIGATION_STEPS = List.of(
            "hydric_stress.step.check_drippers",
            "hydric_stress.step.schedule_emergency_irrigation",
            "hydric_stress.step.measure_stem_potential"
    );

    private static final List<String> HEAT_WAVE_MITIGATION_STEPS = List.of(
            "heat_wave.step.pre_irrigation",
            "heat_wave.step.morning_irrigation",
            "heat_wave.step.suspend_cultural_ops"
    );

    /** Forecast minimum that raises a frost warning, as the requirements set it (US18: ≤ 1.5 °C). */
    public static final double FROST_WARNING_CELSIUS = 1.5;

    /** Maximum that opens a heat wave warning (ADR-006: the single source of the bloom heat threshold). */
    public static final double HEAT_WARNING_CELSIUS = 32.0;

    /** Maximum that raises a heat wave to critical (ADR-006). */
    public static final double HEAT_CRITICAL_CELSIUS = 36.0;

    private static final List<String> FROST_MITIGATION_STEPS = List.of(
            "frost_warning.step.pre_irrigate_soil",
            "frost_warning.step.activate_frost_protection",
            "frost_warning.step.delay_pruning"
    );

    /**
     * Evaluates hydric stress risk against soil texture-specific moisture thresholds at 30cm depth.
     *
     * @param soilMoisturePercentage the measured volumetric soil moisture percentage
     * @param soilTextureType        the soil texture class
     * @return Optional containing {@link IncidentTrigger} if stress detected, empty otherwise
     */
    public Optional<IncidentTrigger> evaluateHydricRisk(double soilMoisturePercentage, SoilTextureType soilTextureType) {
        double criticalThreshold = hydricCriticalThreshold(soilTextureType);
        double warningThreshold = hydricWarningThreshold(soilTextureType);

        if (soilMoisturePercentage < criticalThreshold) {
            var breach = new ThresholdBreachInfo(
                    "SOIL_MOISTURE_30CM",
                    soilMoisturePercentage,
                    criticalThreshold,
                    "%"
            );
            return Optional.of(new IncidentTrigger(
                    IncidentType.HYDRIC_STRESS,
                    IncidentSeverity.CRITICAL,
                    breach,
                    HYDRIC_MITIGATION_STEPS
            ));
        } else if (soilMoisturePercentage < warningThreshold) {
            var breach = new ThresholdBreachInfo(
                    "SOIL_MOISTURE_30CM",
                    soilMoisturePercentage,
                    warningThreshold,
                    "%"
            );
            return Optional.of(new IncidentTrigger(
                    IncidentType.HYDRIC_STRESS,
                    IncidentSeverity.WARNING,
                    breach,
                    HYDRIC_MITIGATION_STEPS
            ));
        }

        return Optional.empty();
    }

    /**
     * Soil moisture at 30 cm below which hydric stress is critical for a soil texture.
     *
     * @param soilTextureType the soil texture class; null is treated as a loam
     * @return the threshold, in volumetric %
     */
    public static double hydricCriticalThreshold(SoilTextureType soilTextureType) {
        if (soilTextureType == SoilTextureType.SANDY) {
            return 12.0;
        } else if (soilTextureType == SoilTextureType.CLAY) {
            return 20.0;
        }
        // LOAM, SILT, NOT_APPLICABLE, or others
        return 16.0;
    }

    /**
     * Soil moisture at 30 cm below which hydric stress is a warning for a soil texture.
     *
     * @param soilTextureType the soil texture class; null is treated as a loam
     * @return the threshold, in volumetric %
     */
    public static double hydricWarningThreshold(SoilTextureType soilTextureType) {
        if (soilTextureType == SoilTextureType.SANDY) {
            return 16.0;
        } else if (soilTextureType == SoilTextureType.CLAY) {
            return 24.0;
        }
        // LOAM, SILT, NOT_APPLICABLE, or others
        return 20.0;
    }

    /**
     * Evaluates thermal heat wave risk based on ambient temperature and humidity.
     *
     * @param temperatureCelsius       measured ambient temperature in degrees Celsius
     * @param relativeHumidityPercentage measured relative humidity percentage
     * @return Optional containing {@link IncidentTrigger} if heat wave conditions are met
     */
    public Optional<IncidentTrigger> evaluateThermalRisk(double temperatureCelsius, double relativeHumidityPercentage) {
        return evaluateHeatRisk(temperatureCelsius);
    }

    /**
     * Evaluates heat wave risk from a temperature alone, such as the highest forecast maximum: the forecast carries no
     * humidity, and the thresholds do not use it.
     *
     * @param temperatureCelsius the temperature in degrees Celsius
     * @return Optional containing {@link IncidentTrigger} if heat wave conditions are met
     */
    public Optional<IncidentTrigger> evaluateHeatRisk(double temperatureCelsius) {
        if (temperatureCelsius >= HEAT_CRITICAL_CELSIUS) {
            var breach = new ThresholdBreachInfo(
                    "AMBIENT_TEMPERATURE",
                    temperatureCelsius,
                    HEAT_CRITICAL_CELSIUS,
                    "°C"
            );
            return Optional.of(new IncidentTrigger(
                    IncidentType.HEAT_WAVE,
                    IncidentSeverity.CRITICAL,
                    breach,
                    HEAT_WAVE_MITIGATION_STEPS
            ));
        } else if (temperatureCelsius >= HEAT_WARNING_CELSIUS) {
            var breach = new ThresholdBreachInfo(
                    "AMBIENT_TEMPERATURE",
                    temperatureCelsius,
                    HEAT_WARNING_CELSIUS,
                    "°C"
            );
            return Optional.of(new IncidentTrigger(
                    IncidentType.HEAT_WAVE,
                    IncidentSeverity.WARNING,
                    breach,
                    HEAT_WAVE_MITIGATION_STEPS
            ));
        }

        return Optional.empty();
    }

    /**
     * Evaluates frost risk based on minimum forecasted or recorded temperature.
     *
     * @param minTemperatureCelsius minimum temperature in degrees Celsius
     * @return Optional containing {@link IncidentTrigger} if frost risk is present
     */
    public Optional<IncidentTrigger> evaluateFrostRisk(double minTemperatureCelsius) {
        if (minTemperatureCelsius <= -2.0) {
            var breach = new ThresholdBreachInfo(
                    "MIN_TEMPERATURE",
                    minTemperatureCelsius,
                    -2.0,
                    "°C"
            );
            return Optional.of(new IncidentTrigger(
                    IncidentType.FROST_WARNING,
                    IncidentSeverity.CRITICAL,
                    breach,
                    FROST_MITIGATION_STEPS
            ));
        } else if (minTemperatureCelsius <= FROST_WARNING_CELSIUS) {
            var breach = new ThresholdBreachInfo(
                    "MIN_TEMPERATURE",
                    minTemperatureCelsius,
                    FROST_WARNING_CELSIUS,
                    "°C"
            );
            return Optional.of(new IncidentTrigger(
                    IncidentType.FROST_WARNING,
                    IncidentSeverity.WARNING,
                    breach,
                    FROST_MITIGATION_STEPS
            ));
        }

        return Optional.empty();
    }
}
