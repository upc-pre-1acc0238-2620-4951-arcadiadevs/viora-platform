package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Reads, through the ACL services, the climate data the ALERT and CLIMATE questions need: the catalog evaluates it
 * and the facts assemblers answer from it. Each screen reads only what it uses, so the forecast (a call to
 * Open-Meteo, cached by Telemetry) is fetched only when a question can use it.
 */
@Component
public class ClimateDataReader {

    /** A reading older than this is not shown as what the sensors say now. */
    static final Duration RECENT_READING = Duration.ofHours(24);

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
        return new ClimateData(thresholds, null, 0, 0, externalTelemetryService.findForecast(plotId).orElse(null),
                sensors.nodes(), recent ? sensors.reading() : null);
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
