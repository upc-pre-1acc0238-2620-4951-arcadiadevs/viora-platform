package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlertFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.springframework.stereotype.Service;

/**
 * Reads the figures of the alert screen: the thresholds of Telemetry, the most severe open alert of the plot, how many
 * plots of its owner share it and, for heat and frost, the forecast. Without an alert nothing else is read, and a
 * hydric alert reads no forecast (a call to Open-Meteo, cached by Telemetry), because it cannot tell its end.
 */
@Service
public class AlertFactsReader implements ScreenFactsReader {

    private final ExternalTelemetryService externalTelemetryService;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the reader.
     *
     * @param externalTelemetryService reads the thresholds, the open alerts and the forecast
     * @param externalOrchardService   lists the plots of the owner
     */
    public AlertFactsReader(ExternalTelemetryService externalTelemetryService,
                            ExternalOrchardService externalOrchardService) {
        this.externalTelemetryService = externalTelemetryService;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.ALERT;
    }

    @Override
    public ScreenFacts read(PlotId plotId, int campaignYear) {
        var thresholds = externalTelemetryService.findThresholds();
        var incident = ClimateData.mostSevere(externalTelemetryService.findOpenIncidents(plotId)).orElse(null);
        if (incident == null) {
            return new AlertFacts(thresholds, null, 0, 0, null);
        }
        var plots = externalOrchardService.findActivePlotIdsOfOwner(plotId);
        int atRisk = externalTelemetryService.countPlotsWithOpenIncident(plots, incident.type());
        boolean forecastTells = ClimateData.HEAT_WAVE.equals(incident.type())
                || ClimateData.FROST_WARNING.equals(incident.type());
        var forecast = forecastTells ? externalTelemetryService.findForecast(plotId).orElse(null) : null;
        return new AlertFacts(thresholds, incident, atRisk, plots.size(), forecast);
    }
}
