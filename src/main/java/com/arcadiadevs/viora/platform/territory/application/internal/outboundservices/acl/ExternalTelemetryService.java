package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DailyMinimum;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Anti-corruption layer reading from Telemetry the open incidents and the forecast minimums of a plot.
 */
@Service("territoryExternalTelemetryService")
public class ExternalTelemetryService {

    private final TelemetryContextFacade telemetryContextFacade;

    /**
     * Creates the service.
     *
     * @param telemetryContextFacade the Telemetry facade
     */
    public ExternalTelemetryService(TelemetryContextFacade telemetryContextFacade) {
        this.telemetryContextFacade = telemetryContextFacade;
    }

    /**
     * Lists the types of the open incidents of a plot with a severity. A snoozed incident is still open: the
     * producer postponed it for themselves, not for the manager.
     *
     * @param plotId   the plot
     * @param severity {@code CRITICAL} or {@code WARNING}
     * @return the incident types
     */
    public List<String> findOpenIncidentTypes(PlotId plotId, String severity) {
        return telemetryContextFacade.findOpenIncidents(plotId.plotId()).stream()
                .filter(incident -> severity.equals(incident.severity()))
                .map(incident -> incident.type())
                .toList();
    }

    /**
     * Lists the forecast minimum of each coming day at a plot.
     *
     * @param plotId the plot
     * @return the daily minimums; empty without a forecast
     */
    public List<DailyMinimum> findForecastMinimums(PlotId plotId) {
        return telemetryContextFacade.findForecastOutlook(plotId.plotId())
                .map(outlook -> outlook.days().stream()
                        .map(day -> new DailyMinimum(day.date(), day.minCelsius()))
                        .toList())
                .orElse(List.of());
    }
}
