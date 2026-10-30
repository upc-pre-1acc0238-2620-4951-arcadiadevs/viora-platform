package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Outbound ACL service used by the assistant to read what Telemetry knows about a plot: its alert thresholds, open
 * alerts, forecast and nodes, already judged by Telemetry.
 */
@Service("assistantExternalTelemetryService")
public class ExternalTelemetryService {

    private final TelemetryContextFacade telemetryContextFacade;

    public ExternalTelemetryService(TelemetryContextFacade telemetryContextFacade) {
        this.telemetryContextFacade = telemetryContextFacade;
    }

    /**
     * The thresholds Telemetry raises its alerts with (ADR-006).
     *
     * @return the thresholds
     */
    public ClimateData.Thresholds findThresholds() {
        var thresholds = telemetryContextFacade.findClimateThresholds();
        return new ClimateData.Thresholds(thresholds.heatWarningCelsius(), thresholds.heatCriticalCelsius(),
                thresholds.frostWarningCelsius());
    }

    /**
     * The open (active or snoozed) alerts of a plot.
     *
     * @param plotId the assistant PlotId value object
     * @return the alerts, the day they were raised in the producers' time zone
     */
    public List<ClimateData.Incident> findOpenIncidents(PlotId plotId) {
        return telemetryContextFacade.findOpenIncidents(plotId.plotId()).stream()
                .map(incident -> new ClimateData.Incident(incident.type(), incident.severity(),
                        incident.currentValue(), incident.thresholdValue(), incident.unit(),
                        incident.triggeredAt().atZone(ProducerTimeZone.ZONE).toLocalDate(), incident.steps(),
                        incident.completedSteps()))
                .toList();
    }

    /**
     * Counts, among the given plots, those with an open alert of a type.
     *
     * @param plotIds      the plots, already limited to one producer
     * @param incidentType the alert type
     * @return the plots with that alert open
     */
    public int countPlotsWithOpenIncident(Collection<String> plotIds, String incidentType) {
        return telemetryContextFacade.countPlotsWithOpenIncident(plotIds, incidentType);
    }

    /**
     * The seven-day forecast of a plot, each day judged against the heat and frost thresholds by Telemetry.
     *
     * @param plotId the assistant PlotId value object
     * @return the forecast, or empty when unavailable
     */
    public Optional<ClimateData.Forecast> findForecast(PlotId plotId) {
        return telemetryContextFacade.findForecastOutlook(plotId.plotId())
                .map(outlook -> new ClimateData.Forecast(
                        outlook.syncedAt().atZone(ProducerTimeZone.ZONE).toLocalDate(),
                        outlook.days().stream()
                                .map(day -> new ClimateData.ForecastDay(day.date(), day.maxCelsius(),
                                        day.minCelsius(), day.heatRisk(), day.frostRisk()))
                                .toList()));
    }

    /**
     * The nodes of a plot and its latest stored reading.
     *
     * @param plotId the assistant PlotId value object
     * @return the nodes and the reading with its hour
     */
    public Sensors findSensors(PlotId plotId) {
        var sensors = telemetryContextFacade.findPlotSensors(plotId.plotId());
        var nodes = sensors.nodes().stream()
                .map(node -> new ClimateData.SensorNode(node.type(), node.depthCm(), node.simulated(),
                        node.criticalMoisturePercent(), node.warningMoisturePercent(), node.moistureRisk()))
                .toList();
        var latest = sensors.latestReading();
        return latest == null
                ? new Sensors(nodes, null, null)
                : new Sensors(nodes, new ClimateData.Reading(latest.temperatureCelsius(), latest.relativeHumidity(),
                        latest.soilMoisturePercent()), latest.readAt());
    }

    /**
     * Nodes and latest reading of a plot.
     *
     * @param nodes   the linked nodes
     * @param reading the latest reading; null without one
     * @param readAt  hour of the reading; null without one
     */
    public record Sensors(List<ClimateData.SensorNode> nodes, ClimateData.Reading reading, Instant readAt) {
    }
}
