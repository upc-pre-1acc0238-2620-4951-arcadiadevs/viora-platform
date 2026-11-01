package com.arcadiadevs.viora.platform.telemetry.interfaces.acl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * ACL facade exposing Telemetry bounded context capabilities to external contexts.
 *
 * <p>Only primitive, published-language structures cross the boundary; Telemetry aggregates and value objects stay
 * internal.</p>
 */
public interface TelemetryContextFacade {

    /**
     * Summarizes how much climate data a plot has over a period, for the agronomic dossier.
     *
     * @param plotId the plot identifier string (UUID)
     * @param from   start of the period, inclusive
     * @param to     end of the period, inclusive
     * @return the linked nodes and the days with at least one hourly reading; zeros when there is none
     */
    TelemetryCoverage findTelemetryCoverage(String plotId, Instant from, Instant to);

    /**
     * The climate thresholds Telemetry raises its alerts with (ADR-006), so other contexts never declare their own.
     *
     * @return the thresholds
     */
    ClimateThresholds findClimateThresholds();

    /**
     * The incidents of a plot that are still open: active or snoozed.
     *
     * @param plotId the plot identifier string (UUID)
     * @return the open incidents; empty for an unknown or malformed plot
     */
    List<OpenIncident> findOpenIncidents(String plotId);

    /**
     * Counts, among the given plots, those with an open incident of a type.
     *
     * @param plotIds      the plots to look at, such as the active plots of one producer
     * @param incidentType the incident type ({@code HEAT_WAVE}, {@code FROST_WARNING}, {@code HYDRIC_STRESS} ...)
     * @return plots with at least one open incident of that type; 0 for an unknown type
     */
    int countPlotsWithOpenIncident(Collection<String> plotIds, String incidentType);

    /**
     * The seven-day forecast of a plot, each day already judged against the heat and frost thresholds by
     * Telemetry. It never raises incidents.
     *
     * @param plotId the plot identifier string (UUID)
     * @return the forecast, or empty when the plot has no location or no forecast is available
     */
    Optional<ForecastOutlook> findForecastOutlook(String plotId);

    /**
     * The nodes linked to a plot and its latest stored reading. It only reads what is stored: it never triggers the
     * virtual node simulation.
     *
     * @param plotId the plot identifier string (UUID)
     * @return the nodes and the latest reading of the last week; empty for an unknown or malformed plot
     */
    PlotSensors findPlotSensors(String plotId);

    /**
     * Climate data available for a plot over a period.
     *
     * @param linkedNodes      nodes linked to the plot and not unlinked
     * @param daysWithReadings distinct UTC days with at least one hourly reading
     */
    record TelemetryCoverage(int linkedNodes, int daysWithReadings) {
    }

    /**
     * Climate thresholds of the alerts.
     *
     * @param heatWarningCelsius  maximum that opens a heat wave warning
     * @param heatCriticalCelsius maximum that raises a heat wave to critical
     * @param frostWarningCelsius minimum that opens a frost warning
     */
    record ClimateThresholds(double heatWarningCelsius, double heatCriticalCelsius, double frostWarningCelsius) {
    }

    /**
     * An open incident of a plot.
     *
     * @param incidentId     incident identifier
     * @param type           incident type
     * @param severity       {@code WARNING} or {@code CRITICAL}
     * @param currentValue   measured or forecast value that breached the threshold
     * @param thresholdValue threshold it breached
     * @param unit           unit of both values ({@code °C}, {@code %})
     * @param triggeredAt    when it was raised
     * @param steps          mitigation steps of the incident
     * @param completedSteps mitigation steps already completed
     */
    record OpenIncident(String incidentId, String type, String severity, double currentValue,
            double thresholdValue, String unit, Instant triggeredAt, int steps, int completedSteps) {
    }

    /**
     * Seven-day forecast of a plot.
     *
     * @param syncedAt when the forecast was fetched
     * @param days     the days, in date order
     */
    record ForecastOutlook(Instant syncedAt, List<ForecastDayOutlook> days) {
    }

    /**
     * One forecast day.
     *
     * @param date       the day
     * @param maxCelsius forecast maximum
     * @param minCelsius forecast minimum
     * @param heatRisk   whether the maximum reaches the heat wave threshold
     * @param frostRisk  whether the minimum reaches the frost threshold
     */
    record ForecastDayOutlook(LocalDate date, double maxCelsius, double minCelsius, boolean heatRisk,
            boolean frostRisk) {
    }

    /**
     * Nodes and latest reading of a plot.
     *
     * @param nodes         active nodes linked to the plot
     * @param latestReading latest reading of the last week; null when there is none
     */
    record PlotSensors(List<LinkedNode> nodes, LatestReading latestReading) {
    }

    /**
     * A node linked to a plot.
     *
     * @param type                    {@code MICROCLIMATE} or {@code SOIL_PROBE}
     * @param depthCm                 installation depth; null when not set
     * @param simulated               whether its readings are simulated (virtual node) rather than measured
     * @param criticalMoisturePercent soil moisture below which hydric stress is critical; null for a microclimate node
     * @param warningMoisturePercent  soil moisture below which hydric stress is a warning; null for a microclimate
     *                                node
     * @param moistureRisk            hydric stress the latest soil moisture shows against those thresholds
     *                                ({@code WARNING}, {@code CRITICAL}); null when none, without a reading or for a
     *                                microclimate node
     */
    record LinkedNode(String type, Integer depthCm, boolean simulated, Double criticalMoisturePercent,
            Double warningMoisturePercent, String moistureRisk) {
    }

    /**
     * Latest stored reading of a plot.
     *
     * @param readAt              hour of the reading
     * @param temperatureCelsius  air temperature
     * @param relativeHumidity    air relative humidity, %
     * @param soilMoisturePercent volumetric soil moisture, %
     */
    record LatestReading(Instant readAt, double temperatureCelsius, double relativeHumidity,
            double soilMoisturePercent) {
    }
}
