package com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.commandservices.AgroclimaticIncidentCommandService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.CompleteMitigationStepCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.NormalizeAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.PostponeAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RaiseAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Application service implementation orchestrating state-changing use cases for Agroclimatic Incidents.
 */
@Service
@Transactional
public class AgroclimaticIncidentCommandServiceImpl implements AgroclimaticIncidentCommandService {

    private final AgroclimaticIncidentRepository incidentRepository;
    private final ExternalOrchardService externalOrchardService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs the AgroclimaticIncidentCommandServiceImpl with required dependencies.
     *
     * @param incidentRepository     domain repository port for incidents
     * @param externalOrchardService outbound ACL service for orchard verifications
     * @param eventPublisher         Spring application event publisher
     */
    public AgroclimaticIncidentCommandServiceImpl(
            AgroclimaticIncidentRepository incidentRepository,
            @Qualifier("telemetryExternalOrchardService") ExternalOrchardService externalOrchardService,
            ApplicationEventPublisher eventPublisher
    ) {
        if (incidentRepository == null) {
            throw new IllegalArgumentException("incident.repository.null");
        }
        if (externalOrchardService == null) {
            throw new IllegalArgumentException("device.external_orchard_service.null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("incident.event_publisher.null");
        }
        this.incidentRepository = incidentRepository;
        this.externalOrchardService = externalOrchardService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<String, ApplicationError> handle(RaiseAgroclimaticIncidentCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "incident.command.null"));
        }
        try {
            var plotId = new PlotId(command.plotId());
            if (!externalOrchardService.existsActivePlot(plotId)) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            var type = IncidentType.from(command.type());
            var severity = IncidentSeverity.from(command.severity());

            var activeOpt = incidentRepository.findActiveByPlotIdAndType(plotId, type);
            if (activeOpt.isPresent()) {
                // Already an active incident for this plot and type; return existing without duplicate
                return Result.success(activeOpt.get().snapshot().id().incidentId());
            }

            var breachInfo = new ThresholdBreachInfo(
                    command.metricName(),
                    command.currentValue(),
                    command.thresholdValue(),
                    command.unit()
            );

            var incident = AgroclimaticIncident.raise(
                    plotId,
                    type,
                    severity,
                    breachInfo,
                    command.mitigationStepInstructionKeys(),
                    Instant.now()
            );

            var saved = incidentRepository.save(incident);
            publishAndClearDomainEvents(incident);

            return Result.success(saved.snapshot().id().incidentId());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("incident-raise", ex.getMessage()));
        }
    }

    @Override
    public Result<String, ApplicationError> handle(NormalizeAgroclimaticIncidentCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "incident.command.null"));
        }
        try {
            var incidentId = new IncidentId(command.incidentId());
            var incidentOpt = incidentRepository.findById(incidentId);
            if (incidentOpt.isEmpty()) {
                return Result.failure(ApplicationError.notFound("Incident", command.incidentId()));
            }

            var incident = incidentOpt.get();
            incident.normalize(command.resolvedAt());

            var saved = incidentRepository.save(incident);
            publishAndClearDomainEvents(incident);

            return Result.success(saved.snapshot().id().incidentId());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("incident-normalize", ex.getMessage()));
        }
    }

    @Override
    public Result<String, ApplicationError> handle(PostponeAgroclimaticIncidentCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "incident.command.null"));
        }
        try {
            var incidentId = new IncidentId(command.incidentId());
            var incidentOpt = incidentRepository.findById(incidentId);
            if (incidentOpt.isEmpty()) {
                return Result.failure(ApplicationError.notFound("Incident", command.incidentId()));
            }

            var incident = incidentOpt.get();
            incident.postpone(Duration.ofHours(command.durationHours()));

            var saved = incidentRepository.save(incident);
            publishAndClearDomainEvents(incident);

            return Result.success(saved.snapshot().id().incidentId());
        } catch (IllegalStateException ex) {
            return Result.failure(ApplicationError.businessRuleViolation("incident-lifecycle", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("incident-postpone", ex.getMessage()));
        }
    }

    @Override
    public Result<String, ApplicationError> handle(CompleteMitigationStepCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "incident.command.null"));
        }
        try {
            var incidentId = new IncidentId(command.incidentId());
            var stepId = new MitigationStepId(command.stepId());

            var incidentOpt = incidentRepository.findById(incidentId);
            if (incidentOpt.isEmpty()) {
                return Result.failure(ApplicationError.notFound("Incident", command.incidentId()));
            }

            var incident = incidentOpt.get();
            incident.completeMitigationStep(stepId, Instant.now());

            var saved = incidentRepository.save(incident);
            publishAndClearDomainEvents(incident);

            return Result.success(saved.snapshot().id().incidentId());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("incident-step-complete", ex.getMessage()));
        }
    }

    private void publishAndClearDomainEvents(AgroclimaticIncident incident) {
        for (var event : incident.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        incident.clearDomainEvents();
    }
}
