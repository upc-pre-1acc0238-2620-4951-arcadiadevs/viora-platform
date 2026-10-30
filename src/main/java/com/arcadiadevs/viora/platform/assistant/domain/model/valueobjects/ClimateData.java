package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Climate data of a plot the ALERT and CLIMATE questions answer from, already judged by Telemetry: its thresholds,
 * the alert the screen is about, the forecast and the nodes. The assistant never evaluates a threshold itself.
 *
 * @param thresholds     the alert thresholds; null when not read (screens without climate questions)
 * @param incident       the most severe open heat, frost or hydric alert of the plot; null without one
 * @param plotsAtRisk    active plots of the producer with an open alert of the same type, this one included
 * @param activePlots    active plots of the producer
 * @param forecast       the seven-day forecast; null when unavailable or not read
 * @param nodes          nodes linked to the plot
 * @param latestReading  latest reading of the plot, only when recent; null otherwise
 */
public record ClimateData(
        Thresholds thresholds,
        Incident incident,
        int plotsAtRisk,
        int activePlots,
        Forecast forecast,
        List<SensorNode> nodes,
        Reading latestReading
) {

    /** No climate data: the screens that do not read Telemetry. */
    public static final ClimateData NONE = new ClimateData(null, null, 0, 0, null, List.of(), null);

    /** Alert types an ALERT screen is opened for; a warm winter belongs to the chill screen. */
    public static final List<String> ALERT_TYPES = List.of("HEAT_WAVE", "FROST_WARNING", "HYDRIC_STRESS");

    public static final String HEAT_WAVE = "HEAT_WAVE";
    public static final String FROST_WARNING = "FROST_WARNING";
    public static final String SOIL_PROBE = "SOIL_PROBE";

    public ClimateData {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
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
     * Soil probes whose readings are measured, not simulated.
     *
     * @return the real probes
     */
    public List<SensorNode> realProbes() {
        return nodes.stream().filter(node -> SOIL_PROBE.equals(node.type()) && !node.simulated()).toList();
    }

    /**
     * Thresholds of the alerts, as Telemetry publishes them (ADR-006).
     *
     * @param heatWarning  maximum that opens a heat wave warning, °C
     * @param heatCritical maximum that raises it to critical, °C
     * @param frostWarning minimum that opens a frost warning, °C
     */
    public record Thresholds(double heatWarning, double heatCritical, double frostWarning) {
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
    }

    /**
     * Latest reading of the plot.
     *
     * @param temperature  air temperature, °C
     * @param humidity     air relative humidity, %
     * @param soilMoisture soil moisture, %
     */
    public record Reading(double temperature, double humidity, double soilMoisture) {
    }
}
