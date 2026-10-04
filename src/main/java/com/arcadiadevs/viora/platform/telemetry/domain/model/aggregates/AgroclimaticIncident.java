package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.entities.MitigationStep;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentNormalizedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentPostponedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.AgroclimaticIncidentRaisedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.events.MitigationStepCompletedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Domain Aggregate Root representing an Agroclimatic Incident.
 *
 * <p>Encapsulates lifecycle transitions (ACTIVE -> SNOOZED -> NORMALIZED),
 * actionable mitigation steps, stress duration accounting, and domain event publication.
 * Completely free of persistence or framework annotations.</p>
 */
public class AgroclimaticIncident extends AbstractDomainAggregateRoot<AgroclimaticIncident> {

    private final IncidentId id;
    private final PlotId plotId;
    private final IncidentType type;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private final ThresholdBreachInfo breachInfo;
    private final Instant triggeredAt;
    private Instant resolvedAt;
    private Instant snoozedUntil;
    private Long stressDurationMinutes;
    private final List<MitigationStep> mitigationSteps;
    private Long revision;

    private AgroclimaticIncident(
            IncidentId id,
            PlotId plotId,
            IncidentType type,
            IncidentSeverity severity,
            IncidentStatus status,
            ThresholdBreachInfo breachInfo,
            Instant triggeredAt,
            Instant resolvedAt,
            Instant snoozedUntil,
            Long stressDurationMinutes,
            List<MitigationStep> mitigationSteps,
            Long revision
    ) {
        this.id = id;
        this.plotId = plotId;
        this.type = type;
        this.severity = severity;
        this.status = status;
        this.breachInfo = breachInfo;
        this.triggeredAt = triggeredAt;
        this.resolvedAt = resolvedAt;
        this.snoozedUntil = snoozedUntil;
        this.stressDurationMinutes = (stressDurationMinutes != null) ? stressDurationMinutes : 0L;
        this.mitigationSteps = new ArrayList<>(mitigationSteps != null ? mitigationSteps : List.of());
        this.revision = revision;
    }

    /**
     * Domain factory method that raises a new agroclimatic incident and records a domain event.
     *
     * @param plotId                      the plot logical reference
     * @param type                        the incident type
     * @param severity                    the severity classification
     * @param breachInfo                  the threshold breach details
     * @param mitigationInstructionKeys   suggested action instruction keys
     * @param triggeredAt                 the trigger timestamp
     * @return a new valid {@link AgroclimaticIncident}
     */
    public static AgroclimaticIncident raise(
            PlotId plotId,
            IncidentType type,
            IncidentSeverity severity,
            ThresholdBreachInfo breachInfo,
            List<String> mitigationInstructionKeys,
            Instant triggeredAt
    ) {
        if (plotId == null) {
            throw new IllegalArgumentException("incident.plot_id.null");
        }
        if (type == null) {
            throw new IllegalArgumentException("incident.type.null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("incident.severity.null");
        }
        if (breachInfo == null) {
            throw new IllegalArgumentException("incident.breach_info.null");
        }

        var incidentId = new IncidentId();
        var triggerTime = (triggeredAt != null) ? triggeredAt : Instant.now();

        List<MitigationStep> steps = new ArrayList<>();
        if (mitigationInstructionKeys != null) {
            for (String key : mitigationInstructionKeys) {
                if (key != null && !key.isBlank()) {
                    steps.add(MitigationStep.create(key));
                }
            }
        }

        var incident = new AgroclimaticIncident(
                incidentId,
                plotId,
                type,
                severity,
                IncidentStatus.ACTIVE,
                breachInfo,
                triggerTime,
                null,
                null,
                0L,
                steps,
                0L
        );

        incident.registerDomainEvent(new AgroclimaticIncidentRaisedEvent(
                incidentId.incidentId(),
                plotId.plotId(),
                type.name(),
                severity.name(),
                triggerTime
        ));

        return incident;
    }

    /**
     * Reconstitutes an aggregate instance from an immutable snapshot without raising domain events.
     *
     * @param snapshot the snapshot source
     * @return the reconstituted aggregate instance
     */
    public static AgroclimaticIncident reconstitute(AgroclimaticIncidentSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("incident.snapshot.null");
        }
        var steps = snapshot.mitigationSteps().stream()
                .map(MitigationStep::reconstitute)
                .toList();

        return new AgroclimaticIncident(
                snapshot.id(),
                snapshot.plotId(),
                snapshot.type(),
                snapshot.severity(),
                snapshot.status(),
                snapshot.breachInfo(),
                snapshot.triggeredAt(),
                snapshot.resolvedAt(),
                snapshot.snoozedUntil(),
                snapshot.stressDurationMinutes(),
                steps,
                snapshot.revision()
        );
    }

    /**
     * Normalizes the incident upon return to optimal agroclimatic conditions.
     *
     * @param resolutionTime the instant of normalization
     */
    public void normalize(Instant resolutionTime) {
        if (this.status == IncidentStatus.NORMALIZED) {
            return;
        }
        this.status = IncidentStatus.NORMALIZED;
        this.resolvedAt = (resolutionTime != null) ? resolutionTime : Instant.now();
        this.stressDurationMinutes = Math.max(0L, Duration.between(this.triggeredAt, this.resolvedAt).toMinutes());

        registerDomainEvent(new AgroclimaticIncidentNormalizedEvent(
                this.id.incidentId(),
                this.plotId.plotId(),
                this.stressDurationMinutes,
                this.resolvedAt
        ));
    }

    /**
     * Postpones (snoozes) notifications for this incident for a given duration.
     *
     * @param duration the duration to snooze
     */
    public void postpone(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("incident.postpone.duration.invalid");
        }
        if (this.status == IncidentStatus.NORMALIZED) {
            throw new IllegalStateException("incident.cannot_postpone_normalized");
        }
        this.status = IncidentStatus.SNOOZED;
        var now = Instant.now();
        this.snoozedUntil = now.plus(duration);

        registerDomainEvent(new AgroclimaticIncidentPostponedEvent(
                this.id.incidentId(),
                this.plotId.plotId(),
                this.snoozedUntil,
                now
        ));
    }

    /**
     * Completes a specific actionable mitigation step within this incident.
     *
     * @param stepId    the step identifier
     * @param timestamp completion instant
     */
    public void completeMitigationStep(MitigationStepId stepId, Instant timestamp) {
        if (stepId == null) {
            throw new IllegalArgumentException("mitigation_step.id.null_or_empty");
        }
        MitigationStep targetStep = null;
        for (MitigationStep step : this.mitigationSteps) {
            if (step.snapshot().id().equals(stepId)) {
                targetStep = step;
                break;
            }
        }
        if (targetStep == null) {
            throw new IllegalArgumentException("mitigation_step.not_found");
        }

        var completionTime = (timestamp != null) ? timestamp : Instant.now();
        targetStep.complete(completionTime);

        registerDomainEvent(new MitigationStepCompletedEvent(
                this.id.incidentId(),
                stepId.stepId(),
                completionTime
        ));
    }

    /**
     * Captures an immutable snapshot of this aggregate root's state.
     *
     * @return the {@link AgroclimaticIncidentSnapshot}
     */
    public AgroclimaticIncidentSnapshot snapshot() {
        var stepSnapshots = this.mitigationSteps.stream()
                .map(MitigationStep::snapshot)
                .toList();

        return new AgroclimaticIncidentSnapshot(
                this.id,
                this.plotId,
                this.type,
                this.severity,
                this.status,
                this.breachInfo,
                this.triggeredAt,
                this.resolvedAt,
                this.snoozedUntil,
                this.stressDurationMinutes,
                stepSnapshots,
                this.revision
        );
    }
}
