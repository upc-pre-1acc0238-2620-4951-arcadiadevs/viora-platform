package com.arcadiadevs.viora.platform.telemetry.application;

import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices.AgroclimaticIncidentCommandServiceImpl;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.CompleteMitigationStepCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.NormalizeAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.PostponeAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.commands.RaiseAgroclimaticIncidentCommand;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentNormalizedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentPostponedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentRaisedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.MitigationStepCompletedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgroclimaticIncidentCommandService Application Unit Tests")
class AgroclimaticIncidentCommandServiceTest {

    @Mock
    private AgroclimaticIncidentRepository incidentRepository;

    @Mock
    private ExternalOrchardService externalOrchardService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AgroclimaticIncidentCommandServiceImpl commandService;

    private final String plotIdStr = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        commandService = new AgroclimaticIncidentCommandServiceImpl(incidentRepository, externalOrchardService, eventPublisher);
    }

    @Test
    @DisplayName("Should raise incident successfully when plot is active")
    void shouldRaiseIncidentSuccessfully() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(incidentRepository.findActiveByPlotIdAndType(any(PlotId.class), any(IncidentType.class))).thenReturn(Optional.empty());
        when(incidentRepository.save(any(AgroclimaticIncident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var command = new RaiseAgroclimaticIncidentCommand(
                plotIdStr,
                "HYDRIC_STRESS",
                "CRITICAL",
                "SOIL_MOISTURE_30CM",
                11.0,
                16.0,
                "%",
                List.of("hydric_stress.step.check_drippers")
        );

        var result = commandService.handle(command);
        assertThat(result).isInstanceOf(Result.Success.class);

        verify(incidentRepository).save(any(AgroclimaticIncident.class));
        verify(eventPublisher).publishEvent(any(AgroclimaticIncidentRaisedEvent.class));
    }

    @Test
    @DisplayName("Should return 404 failure if plot does not exist in orchard")
    void shouldFailWhenPlotDoesNotExist() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(false);

        var command = new RaiseAgroclimaticIncidentCommand(
                plotIdStr,
                "HEAT_WAVE",
                "WARNING",
                "AMBIENT_TEMPERATURE",
                33.0,
                32.0,
                "°C",
                List.of()
        );

        var result = commandService.handle(command);
        assertThat(result).isInstanceOf(Result.Failure.class);
        var failure = (Result.Failure<String, com.arcadiadevs.viora.platform.shared.application.result.ApplicationError>) result;
        assertThat(failure.error().code()).isEqualTo("PLOT_NOT_FOUND");
    }

    @Test
    @DisplayName("Should not duplicate if active incident of same type exists")
    void shouldNotDuplicateWhenActiveIncidentExists() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);

        var existingIncident = AgroclimaticIncident.raise(
                new PlotId(plotIdStr),
                IncidentType.HYDRIC_STRESS,
                IncidentSeverity.CRITICAL,
                new ThresholdBreachInfo("SOIL_MOISTURE_30CM", 11.0, 16.0, "%"),
                List.of(),
                Instant.now()
        );

        when(incidentRepository.findActiveByPlotIdAndType(any(PlotId.class), eq(IncidentType.HYDRIC_STRESS)))
                .thenReturn(Optional.of(existingIncident));

        var command = new RaiseAgroclimaticIncidentCommand(
                plotIdStr,
                "HYDRIC_STRESS",
                "CRITICAL",
                "SOIL_MOISTURE_30CM",
                10.0,
                16.0,
                "%",
                List.of()
        );

        var result = commandService.handle(command);
        assertThat(result).isInstanceOf(Result.Success.class);
        var success = (Result.Success<String, ?>) result;
        assertThat(success.value()).isEqualTo(existingIncident.snapshot().id().incidentId());

        verify(incidentRepository, never()).save(any(AgroclimaticIncident.class));
    }

    @Test
    @DisplayName("Should normalize active incident successfully")
    void shouldNormalizeIncidentSuccessfully() {
        var incident = AgroclimaticIncident.raise(
                new PlotId(plotIdStr),
                IncidentType.HEAT_WAVE,
                IncidentSeverity.WARNING,
                new ThresholdBreachInfo("AMBIENT_TEMPERATURE", 34.0, 32.0, "°C"),
                List.of(),
                Instant.now()
        );
        incident.clearDomainEvents();
        var incidentId = incident.snapshot().id();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(AgroclimaticIncident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var command = new NormalizeAgroclimaticIncidentCommand(incidentId.incidentId(), Instant.now());
        var result = commandService.handle(command);

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(incident.snapshot().status()).isEqualTo(IncidentStatus.NORMALIZED);
        verify(incidentRepository).save(incident);
        verify(eventPublisher).publishEvent(any(AgroclimaticIncidentNormalizedEvent.class));
    }

    @Test
    @DisplayName("Should postpone active incident successfully")
    void shouldPostponeIncidentSuccessfully() {
        var incident = AgroclimaticIncident.raise(
                new PlotId(plotIdStr),
                IncidentType.FROST_WARNING,
                IncidentSeverity.CRITICAL,
                new ThresholdBreachInfo("MIN_TEMPERATURE", -2.5, -2.0, "°C"),
                List.of(),
                Instant.now()
        );
        incident.clearDomainEvents();
        var incidentId = incident.snapshot().id();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(AgroclimaticIncident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var command = new PostponeAgroclimaticIncidentCommand(incidentId.incidentId(), 6L);
        var result = commandService.handle(command);

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(incident.snapshot().status()).isEqualTo(IncidentStatus.SNOOZED);
        verify(incidentRepository).save(incident);
        verify(eventPublisher).publishEvent(any(AgroclimaticIncidentPostponedEvent.class));
    }

    @Test
    @DisplayName("Should complete mitigation step successfully")
    void shouldCompleteMitigationStepSuccessfully() {
        var incident = AgroclimaticIncident.raise(
                new PlotId(plotIdStr),
                IncidentType.HYDRIC_STRESS,
                IncidentSeverity.CRITICAL,
                new ThresholdBreachInfo("SOIL_MOISTURE_30CM", 10.0, 16.0, "%"),
                List.of("hydric_stress.step.check_drippers"),
                Instant.now()
        );
        incident.clearDomainEvents();
        var incidentId = incident.snapshot().id();
        var stepId = incident.snapshot().mitigationSteps().get(0).id();

        when(incidentRepository.findById(incidentId)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(AgroclimaticIncident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var command = new CompleteMitigationStepCommand(incidentId.incidentId(), stepId.stepId());
        var result = commandService.handle(command);

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(incident.snapshot().mitigationSteps().get(0).completed()).isTrue();
        verify(incidentRepository).save(incident);
        verify(eventPublisher).publishEvent(any(MitigationStepCompletedEvent.class));
    }
}
