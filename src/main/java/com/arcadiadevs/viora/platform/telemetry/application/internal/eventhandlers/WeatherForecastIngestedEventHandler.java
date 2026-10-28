package com.arcadiadevs.viora.platform.telemetry.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.telemetry.application.commandservices.AgroclimaticIncidentCommandService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RaiseAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.WeatherForecastIngestedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.services.AgroclimaticThresholdEvaluator;
import com.arcadiadevs.viora.platform.telemetry.domain.services.IncidentTrigger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event handler evaluating agroclimatic risk when new weather forecasts are ingested.
 */
@Component
public class WeatherForecastIngestedEventHandler {

    private final AgroclimaticIncidentCommandService incidentCommandService;
    private final AgroclimaticThresholdEvaluator thresholdEvaluator;

    /**
     * Constructs the event handler injecting the incident command service.
     *
     * @param incidentCommandService the incident command service
     */
    public WeatherForecastIngestedEventHandler(AgroclimaticIncidentCommandService incidentCommandService) {
        if (incidentCommandService == null) {
            throw new IllegalArgumentException("incident.command_service.null");
        }
        this.incidentCommandService = incidentCommandService;
        this.thresholdEvaluator = new AgroclimaticThresholdEvaluator();
    }

    /**
     * Listens for ingested forecast events and raises agroclimatic incidents when the forecast reaches the frost or
     * the heat wave thresholds. Water stress is not judged here: it needs a soil probe, and the virtual node only
     * estimates soil moisture without knowing the irrigation.
     *
     * @param event the weather forecast ingested event
     */
    @EventListener
    public void on(WeatherForecastIngestedEvent event) {
        if (event == null || event.plotId() == null) {
            return;
        }

        thresholdEvaluator.evaluateFrostRisk(event.minTemp()).ifPresent(trigger -> raise(event, trigger));
        thresholdEvaluator.evaluateHeatRisk(event.maxTemp()).ifPresent(trigger -> raise(event, trigger));
    }

    private void raise(WeatherForecastIngestedEvent event, IncidentTrigger trigger) {
        incidentCommandService.handle(new RaiseAgroclimaticIncidentCommand(
                event.plotId(),
                trigger.type().name(),
                trigger.severity().name(),
                trigger.breachInfo().metricName(),
                trigger.breachInfo().currentValue(),
                trigger.breachInfo().thresholdValue(),
                trigger.breachInfo().unit(),
                trigger.mitigationInstructionKeys()
        ));
    }
}
