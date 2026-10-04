package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.telemetry.domain.model.entities.MitigationStepSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.List;

/**
 * Immutable snapshot representing the state of an {@link AgroclimaticIncident} aggregate root.
 *
 * @param id                    the unique incident identity
 * @param plotId                the cross-context reference to the orchard plot
 * @param type                  the agroclimatic incident type
 * @param severity              the severity classification
 * @param status                the current operational status
 * @param breachInfo            the metric details that triggered the incident
 * @param triggeredAt           the timestamp when the incident was triggered
 * @param resolvedAt            the timestamp when the incident was normalized, or null
 * @param snoozedUntil          the timestamp until which notifications are snoozed, or null
 * @param stressDurationMinutes the total duration of stress in minutes
 * @param mitigationSteps       the list of mitigation steps
 * @param revision              the optimistic locking version
 */
public record AgroclimaticIncidentSnapshot(
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
        List<MitigationStepSnapshot> mitigationSteps,
        Long revision
) {
    public AgroclimaticIncidentSnapshot {
        mitigationSteps = (mitigationSteps != null) ? List.copyOf(mitigationSteps) : List.of();
    }
}
