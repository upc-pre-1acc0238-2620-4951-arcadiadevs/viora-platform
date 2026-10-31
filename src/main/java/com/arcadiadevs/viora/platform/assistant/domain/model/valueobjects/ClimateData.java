package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The climate figures of a plot the ALERT and CLIMATE screens read, already judged by Telemetry: its thresholds, its
 * open alerts, the forecast and the nodes. The assistant never evaluates a threshold itself.
 */
public final class ClimateData {

    /** Alert types an ALERT screen is opened for; a warm winter belongs to the chill screen. */
    public static final List<String> ALERT_TYPES = List.of("HEAT_WAVE", "FROST_WARNING", "HYDRIC_STRESS");

    public static final String HEAT_WAVE = "HEAT_WAVE";
    public static final String FROST_WARNING = "FROST_WARNING";
    public static final String SOIL_PROBE = "SOIL_PROBE";

    private ClimateData() {
    }

    /**
     * The alert an ALERT screen is about: the most severe open one (critical first), then the most recent, then
     * heat before frost before hydric stress.
     *
     * @param open the open alerts of the plot
     * @return the alert, or empty without an open heat, frost or hydric alert
     */
    public static Optional<Incident> mostSevere(List<Incident> open) {
        return open.stream()
                .filter(incident -> ALERT_TYPES.contains(incident.type()))
                .min(Comparator.comparing((Incident incident) -> !"CRITICAL".equals(incident.severity()))
                        .thenComparing(Incident::triggeredOn, Comparator.reverseOrder())
                        .thenComparing(incident -> ALERT_TYPES.indexOf(incident.type())));
    }

    /**
     * Thresholds of the alerts, as Telemetry publishes them (ADR-006).
     *
     * @param heatWarning  maximum that opens a heat wave warning, °C
     * @param heatCritical maximum that raises it to critical, °C
     * @param frostWarning minimum that opens a frost warning, °C
     */
    public record Thresholds(double heatWarning, double heatCritical, double frostWarning) {

        String canonical() {
            return heatWarning + ";" + heatCritical + ";" + frostWarning;
        }
    }

    /**
     * An open alert of the plot.
     *
     * @param type           {@code HEAT_WAVE}, {@code FROST_WARNING} or {@code HYDRIC_STRESS}
     * @param severity       {@code WARNING} or {@code CRITICAL}
     * @param value          value that breached the threshold
     * @param threshold      threshold it breached
     * @param unit           unit of both values
     * @param triggeredOn    day it was raised, in the producers' time zone
     * @param steps          mitigation steps
     * @param completedSteps mitigation steps completed
     */
    public record Incident(String type, String severity, double value, double threshold, String unit,
                           LocalDate triggeredOn, int steps, int completedSteps) {

        String canonical() {
            return type + ";" + severity + ";" + value + ";" + threshold + ";" + unit + ";" + triggeredOn + ";"
                    + steps + ";" + completedSteps;
        }
    }

    /**
     * Seven-day forecast.
     *
     * @param syncedOn day it was fetched, in the producers' time zone
     * @param days     the days in date order
     */
    public record Forecast(LocalDate syncedOn, List<ForecastDay> days) {

        public Forecast {
            days = days == null ? List.of() : List.copyOf(days);
        }

        String canonical() {
            return syncedOn + ";" + days.stream().map(ForecastDay::canonical).collect(Collectors.joining(","));
        }
    }

    /**
     * One forecast day, judged by Telemetry.
     *
     * @param date      the day
     * @param max       forecast maximum, °C
     * @param min       forecast minimum, °C
     * @param heatRisk  whether the maximum reaches the heat threshold
     * @param frostRisk whether the minimum reaches the frost threshold
     */
    public record ForecastDay(LocalDate date, double max, double min, boolean heatRisk, boolean frostRisk) {

        String canonical() {
            return date + "/" + max + "/" + min + "/" + heatRisk + "/" + frostRisk;
        }
    }

    /**
     * A node linked to the plot.
     *
     * @param type             {@code MICROCLIMATE} or {@code SOIL_PROBE}
     * @param depthCm          installation depth; null when not set
     * @param simulated        whether its readings are simulated (virtual node)
     * @param criticalMoisture soil moisture of critical hydric stress; null for a microclimate node
     * @param warningMoisture  soil moisture of a hydric stress warning; null for a microclimate node
     * @param moistureRisk     hydric stress Telemetry sees in the latest soil moisture ({@code WARNING},
     *                         {@code CRITICAL}); null when none or for a microclimate node
     */
    public record SensorNode(String type, Integer depthCm, boolean simulated, Double criticalMoisture,
                             Double warningMoisture, String moistureRisk) {

        String canonical() {
            return type + "/" + depthCm + "/" + simulated + "/" + criticalMoisture + "/" + warningMoisture + "/"
                    + moistureRisk;
        }
    }

    /**
     * Latest reading of the plot.
     *
     * @param temperature  air temperature, °C
     * @param humidity     air relative humidity, %
     * @param soilMoisture soil moisture, %
     */
    public record Reading(double temperature, double humidity, double soilMoisture) {

        String canonical() {
            return temperature + ";" + humidity + ";" + soilMoisture;
        }
    }
}
