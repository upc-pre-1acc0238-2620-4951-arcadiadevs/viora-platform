package com.arcadiadevs.viora.platform.telemetry.domain.model;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentNormalizedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentPostponedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentRaisedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.MitigationStepCompletedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AgroclimaticIncident Aggregate Root Unit Tests")
class AgroclimaticIncidentTest {

    private final PlotId validPlotId = new PlotId(UUID.randomUUID().toString());
    private final ThresholdBreachInfo validBreach = new ThresholdBreachInfo(
            "SOIL_MOISTURE_30CM",
            10.5,
            16.0,
            "%"
    );

    @Nested
    @DisplayName("Creation and Invariant Tests")
    class CreationTests {

        @Test
        @DisplayName("Should successfully raise an agroclimatic incident with domain event")
        void shouldRaiseAgroclimaticIncidentSuccessfully() {
            var keys = List.of("hydric_stress.step.check_drippers", "hydric_stress.step.schedule_emergency_irrigation");
            var triggeredAt = Instant.now();

            var incident = AgroclimaticIncident.raise(
                    validPlotId,
                    IncidentType.HYDRIC_STRESS,
                    IncidentSeverity.CRITICAL,
                    validBreach,
                    keys,
                    triggeredAt
            );

            var snapshot = incident.snapshot();
            assertThat(snapshot.id()).isNotNull();
            assertThat(snapshot.plotId()).isEqualTo(validPlotId);
            assertThat(snapshot.type()).isEqualTo(IncidentType.HYDRIC_STRESS);
            assertThat(snapshot.severity()).isEqualTo(IncidentSeverity.CRITICAL);
            assertThat(snapshot.status()).isEqualTo(IncidentStatus.ACTIVE);
            assertThat(snapshot.breachInfo()).isEqualTo(validBreach);
            assertThat(snapshot.triggeredAt()).isEqualTo(triggeredAt);
            assertThat(snapshot.resolvedAt()).isNull();
            assertThat(snapshot.snoozedUntil()).isNull();
            assertThat(snapshot.stressDurationMinutes()).isZero();
            assertThat(snapshot.mitigationSteps()).hasSize(2);
            assertThat(snapshot.mitigationSteps().get(0).completed()).isFalse();

            // Verify domain event
            assertThat(incident.domainEvents()).hasSize(1);
            var event = incident.domainEvents().iterator().next();
            assertThat(event).isInstanceOf(AgroclimaticIncidentRaisedEvent.class);
            var raisedEvent = (AgroclimaticIncidentRaisedEvent) event;
            assertThat(raisedEvent.incidentId()).isEqualTo(snapshot.id().incidentId());
            assertThat(raisedEvent.plotId()).isEqualTo(validPlotId.plotId());
            assertThat(raisedEvent.type()).isEqualTo("HYDRIC_STRESS");
            assertThat(raisedEvent.severity()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when plotId is null")
        void shouldThrowWhenPlotIdIsNull() {
            assertThatThrownBy(() -> AgroclimaticIncident.raise(
                    null,
                    IncidentType.HEAT_WAVE,
                    IncidentSeverity.WARNING,
                    validBreach,
                    List.of(),
                    Instant.now()
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("incident.plot_id.null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when type is null")
        void shouldThrowWhenTypeIsNull() {
            assertThatThrownBy(() -> AgroclimaticIncident.raise(
                    validPlotId,
                    null,
                    IncidentSeverity.WARNING,
                    validBreach,
                    List.of(),
                    Instant.now()
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("incident.type.null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when breachInfo is null")
        void shouldThrowWhenBreachInfoIsNull() {
            assertThatThrownBy(() -> AgroclimaticIncident.raise(
                    validPlotId,
                    IncidentType.FROST_WARNING,
                    IncidentSeverity.CRITICAL,
                    null,
                    List.of(),
                    Instant.now()
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("incident.breach_info.null");
        }
    }

    @Nested
    @DisplayName("Lifecycle Transitions Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Should normalize incident and calculate stress duration in minutes")
        void shouldNormalizeIncidentAndCalculateDuration() {
            var triggeredAt = Instant.now().minus(Duration.ofMinutes(150));
            var incident = AgroclimaticIncident.raise(
                    validPlotId,
                    IncidentType.HYDRIC_STRESS,
                    IncidentSeverity.WARNING,
                    validBreach,
                    List.of(),
                    triggeredAt
            );
            incident.clearDomainEvents();

            var normalizedAt = Instant.now();
            incident.normalize(normalizedAt);

            var snapshot = incident.snapshot();
            assertThat(snapshot.status()).isEqualTo(IncidentStatus.NORMALIZED);
            assertThat(snapshot.resolvedAt()).isEqualTo(normalizedAt);
            assertThat(snapshot.stressDurationMinutes()).isGreaterThanOrEqualTo(149L);

            assertThat(incident.domainEvents()).hasSize(1);
            assertThat(incident.domainEvents().iterator().next()).isInstanceOf(AgroclimaticIncidentNormalizedEvent.class);
        }

        @Test
        @DisplayName("Normalizing an already normalized incident should be idempotent")
        void normalizingAlreadyNormalizedShouldBeIdempotent() {
            var incident = AgroclimaticIncident.raise(
                    validPlotId,
                    IncidentType.HEAT_WAVE,
                    IncidentSeverity.CRITICAL,
                    validBreach,
                    List.of(),
                    Instant.now().minus(Duration.ofHours(2))
            );
            incident.normalize(Instant.now());
            incident.clearDomainEvents();

            incident.normalize(Instant.now());
            assertThat(incident.domainEvents()).isEmpty();
            assertThat(incident.snapshot().status()).isEqualTo(IncidentStatus.NORMALIZED);
        }

        @Test
        @DisplayName("Should postpone (snooze) incident and publish postponed event")
        void shouldPostponeIncident() {
            var incident = AgroclimaticIncident.raise(
                    validPlotId,
                    IncidentType.HEAT_WAVE,
                    IncidentSeverity.WARNING,
                    validBreach,
                    List.of(),
                    Instant.now()
            );
            incident.clearDomainEvents();

            incident.postpone(Duration.ofHours(4));

            var snapshot = incident.snapshot();
            assertThat(snapshot.status()).isEqualTo(IncidentStatus.SNOOZED);
            assertThat(snapshot.snoozedUntil()).isAfter(Instant.now());

            assertThat(incident.domainEvents()).hasSize(1);
            assertThat(incident.domainEvents().iterator().next()).isInstanceOf(AgroclimaticIncidentPostponedEvent.class);
        }

        @Test
        @DisplayName("Should throw when postponing a normalized incident")
        void shouldThrowWhenPostponingNormalizedIncident() {
            var incident = AgroclimaticIncident.raise(
                    validPlotId,
                    IncidentType.HEAT_WAVE,
                    IncidentSeverity.WARNING,
                    validBreach,
                    List.of(),
                    Instant.now()
            );
            incident.normalize(Instant.now());

            assertThatThrownBy(() -> incident.postpone(Duration.ofHours(2)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("incident.cannot_postpone_normalized");
        }
    }

    @Nested
    @DisplayName("Mitigation Steps Tests")
    class MitigationStepsTests {

        @Test
        @DisplayName("Should complete mitigation step and publish domain event")
        void shouldCompleteMitigationStep() {
            var incident = AgroclimaticIncident.raise(
                    validPlotId,
                    IncidentType.HYDRIC_STRESS,
                    IncidentSeverity.CRITICAL,
                    validBreach,
                    List.of("hydric_stress.step.check_drippers"),
                    Instant.now()
            );
            var stepId = incident.snapshot().mitigationSteps().get(0).id();
            incident.clearDomainEvents();

            var completionTime = Instant.now();
            incident.completeMitigationStep(stepId, completionTime);

            var updatedStep = incident.snapshot().mitigationSteps().get(0);
            assertThat(updatedStep.completed()).isTrue();
            assertThat(updatedStep.completedAt()).isEqualTo(completionTime);

            assertThat(incident.domainEvents()).hasSize(1);
            var event = incident.domainEvents().iterator().next();
            assertThat(event).isInstanceOf(MitigationStepCompletedEvent.class);
            var completedEvent = (MitigationStepCompletedEvent) event;
            assertThat(completedEvent.incidentId()).isEqualTo(incident.snapshot().id().incidentId());
            assertThat(completedEvent.stepId()).isEqualTo(stepId.stepId());
        }

        @Test
        @DisplayName("Should throw when stepId is not found in incident")
        void shouldThrowWhenStepNotFound() {
            var incident = AgroclimaticIncident.raise(
                    validPlotId,
                    IncidentType.HYDRIC_STRESS,
                    IncidentSeverity.CRITICAL,
                    validBreach,
                    List.of("hydric_stress.step.check_drippers"),
                    Instant.now()
            );

            var randomStepId = new MitigationStepId();
            assertThatThrownBy(() -> incident.completeMitigationStep(randomStepId, Instant.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("mitigation_step.not_found");
        }
    }

    @Nested
    @DisplayName("Snapshot and Reconstitution Tests")
    class ReconstitutionTests {

        @Test
        @DisplayName("Should reconstitute aggregate accurately from snapshot without events")
        void shouldReconstituteAccurately() {
            var incident = AgroclimaticIncident.raise(
                    validPlotId,
                    IncidentType.FROST_WARNING,
                    IncidentSeverity.CRITICAL,
                    new ThresholdBreachInfo("MIN_TEMPERATURE", -2.5, -2.0, "°C"),
                    List.of("frost_warning.step.activate_frost_protection"),
                    Instant.now()
            );
            var originalSnapshot = incident.snapshot();

            var reconstituted = AgroclimaticIncident.reconstitute(originalSnapshot);
            assertThat(reconstituted.domainEvents()).isEmpty();
            assertThat(reconstituted.snapshot()).isEqualTo(originalSnapshot);
        }
    }
}
