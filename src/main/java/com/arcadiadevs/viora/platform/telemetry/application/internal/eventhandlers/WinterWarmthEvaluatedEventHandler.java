package com.arcadiadevs.viora.platform.telemetry.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.phenology.domain.model.events.WinterWarmthEvaluatedEvent;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.AgroclimaticIncidentCommandService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.NormalizeAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RaiseAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns the winter warmth evaluations of Phenology into the warm winter alert of the alerts center (US23).
 *
 * <p>While a warm spell is running the plot has one open {@link IncidentType#WARM_WINTER} incident (active or
 * postponed, never two); when the spell ends, or the season does, that incident is normalized.</p>
 */
@Component
public class WinterWarmthEvaluatedEventHandler {

    /** Daily maximum of the last evaluated day, against the warm day threshold. */
    static final String METRIC = "DAILY_MAX_TEMPERATURE";

    static final List<String> MITIGATION_STEPS = List.of(
            "warm_winter.step.follow_chill",
            "warm_winter.step.wait_for_bloom",
            "warm_winter.step.record_full_bloom"
    );

    private final AgroclimaticIncidentCommandService commandService;
    private final AgroclimaticIncidentRepository incidentRepository;

    public WinterWarmthEvaluatedEventHandler(AgroclimaticIncidentCommandService commandService,
            AgroclimaticIncidentRepository incidentRepository) {
        this.commandService = commandService;
        this.incidentRepository = incidentRepository;
    }

    @EventListener
    public void on(WinterWarmthEvaluatedEvent event) {
        if (event == null) {
            return;
        }
        var plotId = new PlotId(event.plotId());
        var open = incidentRepository.findByPlotId(plotId).stream()
                .filter(incident -> incident.snapshot().type() == IncidentType.WARM_WINTER)
                .filter(incident -> incident.snapshot().status() != IncidentStatus.NORMALIZED)
                .toList();

        if (event.isWarmSpellActive()) {
            if (open.isEmpty()) {
                commandService.handle(new RaiseAgroclimaticIncidentCommand(
                        event.plotId(),
                        IncidentType.WARM_WINTER.name(),
                        IncidentSeverity.CRITICAL.name(),
                        METRIC,
                        event.latestMaxTemperatureCelsius() == null
                                ? event.warmDayThresholdCelsius() : event.latestMaxTemperatureCelsius(),
                        event.warmDayThresholdCelsius(),
                        "°C",
                        MITIGATION_STEPS));
            }
            return;
        }
        open.forEach(incident -> commandService.handle(new NormalizeAgroclimaticIncidentCommand(
                incident.snapshot().id().incidentId(), event.occurredOn())));
    }
}
