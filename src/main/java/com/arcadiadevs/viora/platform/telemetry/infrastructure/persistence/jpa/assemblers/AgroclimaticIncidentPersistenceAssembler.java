package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncidentSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.entities.MitigationStepSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.embeddables.ThresholdBreachInfoPersistenceEmbeddable;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.AgroclimaticIncidentPersistenceEntity;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.MitigationStepPersistenceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unified persistence assembler translating between the domain {@link AgroclimaticIncident} aggregate
 * and the JPA {@link AgroclimaticIncidentPersistenceEntity}.
 */
public final class AgroclimaticIncidentPersistenceAssembler {

    private AgroclimaticIncidentPersistenceAssembler() {
    }

    /**
     * Converts a domain aggregate into a new JPA persistence entity.
     *
     * @param domain the domain aggregate to map
     * @return the corresponding JPA persistence entity
     */
    public static AgroclimaticIncidentPersistenceEntity toPersistenceFromDomain(AgroclimaticIncident domain) {
        if (domain == null) {
            throw new IllegalArgumentException("incident.snapshot.null");
        }
        var snap = domain.snapshot();

        var entity = new AgroclimaticIncidentPersistenceEntity();
        entity.setId(UUID.fromString(snap.id().incidentId()));
        entity.setPlotId(snap.plotId());
        entity.setType(snap.type());
        entity.setSeverity(snap.severity());
        entity.setStatus(snap.status());

        var breach = snap.breachInfo();
        entity.setBreachInfo(new ThresholdBreachInfoPersistenceEmbeddable(
                breach.metricName(),
                breach.currentValue(),
                breach.thresholdValue(),
                breach.unit()
        ));

        entity.setTriggeredAt(snap.triggeredAt());
        entity.setResolvedAt(snap.resolvedAt());
        entity.setSnoozedUntil(snap.snoozedUntil());
        entity.setStressDurationMinutes(snap.stressDurationMinutes());

        if (snap.revision() != null && snap.revision() > 0L) {
            entity.setRevision(snap.revision());
        }

        List<MitigationStepPersistenceEntity> stepEntities = new ArrayList<>();
        for (MitigationStepSnapshot stepSnap : snap.mitigationSteps()) {
            var stepEntity = new MitigationStepPersistenceEntity();
            stepEntity.setId(UUID.fromString(stepSnap.id().stepId()));
            stepEntity.setIncident(entity);
            stepEntity.setInstructionKey(stepSnap.instructionKey());
            stepEntity.setCompleted(stepSnap.completed());
            stepEntity.setCompletedAt(stepSnap.completedAt());
            stepEntities.add(stepEntity);
        }
        entity.setMitigationSteps(stepEntities);

        return entity;
    }

    /**
     * Updates an existing managed JPA entity from a domain aggregate.
     *
     * @param target the managed target JPA entity
     * @param domain the domain aggregate containing updated state
     * @return the updated target entity
     */
    public static AgroclimaticIncidentPersistenceEntity updateEntityFromDomain(
            AgroclimaticIncidentPersistenceEntity target,
            AgroclimaticIncident domain
    ) {
        if (target == null || domain == null) {
            throw new IllegalArgumentException("incident.snapshot.null");
        }
        var snap = domain.snapshot();
        target.setSeverity(snap.severity());
        target.setStatus(snap.status());
        target.setResolvedAt(snap.resolvedAt());
        target.setSnoozedUntil(snap.snoozedUntil());
        target.setStressDurationMinutes(snap.stressDurationMinutes());

        target.getMitigationSteps().clear();
        for (MitigationStepSnapshot stepSnap : snap.mitigationSteps()) {
            var stepEntity = new MitigationStepPersistenceEntity();
            stepEntity.setId(UUID.fromString(stepSnap.id().stepId()));
            stepEntity.setIncident(target);
            stepEntity.setInstructionKey(stepSnap.instructionKey());
            stepEntity.setCompleted(stepSnap.completed());
            stepEntity.setCompletedAt(stepSnap.completedAt());
            target.getMitigationSteps().add(stepEntity);
        }
        return target;
    }

    /**
     * Reconstitutes a domain {@link AgroclimaticIncident} aggregate from a JPA persistence entity.
     *
     * @param entity the JPA entity source
     * @return the reconstituted pure domain aggregate
     */
    public static AgroclimaticIncident toDomainFromPersistence(AgroclimaticIncidentPersistenceEntity entity) {
        if (entity == null) {
            return null;
        }

        var breachEmbeddable = entity.getBreachInfo();
        var breachInfo = new ThresholdBreachInfo(
                breachEmbeddable.getMetricName(),
                breachEmbeddable.getCurrentValue(),
                breachEmbeddable.getThresholdValue(),
                breachEmbeddable.getUnit()
        );

        List<MitigationStepSnapshot> stepSnapshots = new ArrayList<>();
        if (entity.getMitigationSteps() != null) {
            for (MitigationStepPersistenceEntity stepEntity : entity.getMitigationSteps()) {
                stepSnapshots.add(new MitigationStepSnapshot(
                        new MitigationStepId(stepEntity.getId().toString()),
                        stepEntity.getInstructionKey(),
                        stepEntity.isCompleted(),
                        stepEntity.getCompletedAt()
                ));
            }
        }

        var snapshot = new AgroclimaticIncidentSnapshot(
                new IncidentId(entity.getId().toString()),
                entity.getPlotId(),
                entity.getType(),
                entity.getSeverity(),
                entity.getStatus(),
                breachInfo,
                entity.getTriggeredAt(),
                entity.getResolvedAt(),
                entity.getSnoozedUntil(),
                entity.getStressDurationMinutes(),
                stepSnapshots,
                entity.getRevision() != null ? entity.getRevision() : 0L
        );

        return AgroclimaticIncident.reconstitute(snapshot);
    }
}
