package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.springframework.stereotype.Component;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Reads, through the ACL services, the climate data the ALERT, CLIMATE and SENSORS questions need: the catalog evaluates it
 * and the facts assemblers answer from it. Each screen reads only what it uses, so the forecast (a call to
 * Open-Meteo, cached by Telemetry) is fetched only when a question can use it.
 */
@Component
public class ClimateDataReader {

    /** A reading older than this is not shown as what the sensors say now. */
    static final Duration RECENT_READING = Duration.ofHours(24);

    /** Days, today included, the SENSORS screen counts readings over: the week Telemetry keeps the latest one from. */
    static final int COVERAGE_DAYS = 7;

    private final ExternalTelemetryService externalTelemetryService;
    private final ExternalOrchardService externalOrchardService;
    private final Clock clock;

    public ClimateDataReader(ExternalTelemetryService externalTelemetryService,
            ExternalOrchardService externalOrchardService, Clock clock) {
        this.externalTelemetryService = externalTelemetryService;
        this.externalOrchardService = externalOrchardService;
        this.clock = clock;
    }

    /**
     * Reads the climate data of a plot for the questions of a screen.
     *
     * @param screen the screen; screens without climate questions read nothing
     * @param plotId the plot
     * @return the climate data
     */
    public ClimateData read(ScreenKey screen, PlotId plotId) {
        if (!screen.readsClimate()) {
            return ClimateData.NONE;
        }
        var thresholds = externalTelemetryService.findThresholds();
        if (screen == ScreenKey.ALERT) {
            return alert(plotId, thresholds);
        }
        var sensors = externalTelemetryService.findSensors(plotId);
        var recent = sensors.readAt() != null
                && !sensors.readAt().isBefore(clock.instant().minus(RECENT_READING));
        if (screen == ScreenKey.SENSORS) {
            // The nodes and how much they read; no forecast
            return new ClimateData(thresholds, null, 0, 0, null, sensors.nodes(), recent ? sensors.reading() : null,
                    coverage(plotId, sensors.readAt()));
        }
        return new ClimateData(thresholds, null, 0, 0, externalTelemetryService.findForecast(plotId).orElse(null),
                sensors.nodes(), recent ? sensors.reading() : null);
    }

    private ClimateData.Coverage coverage(PlotId plotId, Instant readAt) {
        var today = LocalDate.now(clock.withZone(ProducerTimeZone.ZONE));
        var from = today.minusDays(COVERAGE_DAYS - 1L).atStartOfDay(ProducerTimeZone.ZONE).toInstant();
        int days = externalTelemetryService.countDaysWithReadings(plotId, from, clock.instant());
        var lastReadingOn = readAt == null ? null : LocalDate.ofInstant(readAt, ProducerTimeZone.ZONE);
        return new ClimateData.Coverage(lastReadingOn, days, COVERAGE_DAYS);
    }

    private ClimateData alert(PlotId plotId, ClimateData.Thresholds thresholds) {
        var incident = ClimateData.mostSevere(externalTelemetryService.findOpenIncidents(plotId)).orElse(null);
        if (incident == null) {
            return new ClimateData(thresholds, null, 0, 0, null, List.of(), null);
        }
        var plots = externalOrchardService.findActivePlotIdsOfOwner(plotId);
        int atRisk = externalTelemetryService.countPlotsWithOpenIncident(plots, incident.type());
        boolean forecastTells = ClimateData.HEAT_WAVE.equals(incident.type())
                || ClimateData.FROST_WARNING.equals(incident.type());
        var forecast = forecastTells ? externalTelemetryService.findForecast(plotId).orElse(null) : null;
        return new ClimateData(thresholds, incident, atRisk, plots.size(), forecast, List.of(), null);
    }
}
