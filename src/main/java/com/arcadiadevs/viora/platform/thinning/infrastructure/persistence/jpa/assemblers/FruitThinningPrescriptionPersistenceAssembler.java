package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.SamplingRoundSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.TreeSamplingRecordSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.FruitThinningPrescriptionPersistenceEntity;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.SamplingRoundPersistenceEntity;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.TreeSamplingRecordPersistenceEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.ExecutionConfirmationSnapshot;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.ExecutionConfirmationPersistenceEntity;

/**
 * Unified persistence assembler bridging domain {@link FruitThinningPrescription} aggregate roots
 * and JPA {@link FruitThinningPrescriptionPersistenceEntity} representations.
 */
public final class FruitThinningPrescriptionPersistenceAssembler {

    private FruitThinningPrescriptionPersistenceAssembler() {
    }

    /**
     * Converts a domain aggregate to a new JPA persistence entity.
     *
     * @param domain the domain aggregate
     * @return the mapped JPA entity
     */
    public static FruitThinningPrescriptionPersistenceEntity toPersistenceFromDomain(FruitThinningPrescription domain) {
        if (domain == null) {
            throw new IllegalArgumentException("thinning.prescription.snapshot.null");
        }
        var snap = domain.snapshot();
        var entity = new FruitThinningPrescriptionPersistenceEntity();
        entity.setId(UUID.fromString(snap.id().prescriptionId()));
        entity.setPlotId(UUID.fromString(snap.plotId().plotId()));
        entity.setCampaignYear(snap.campaignYear().value());
        entity.setObservedPlotRevision(snap.observedPlotRevision());
        entity.setStatus(snap.status().name());
        if (snap.sustainableLoad() != null) {
            entity.setTargetFruitsPerShoot(snap.sustainableLoad().targetFruitsPerShoot());
            entity.setPercentageToRemove(snap.sustainableLoad().percentageToRemove());
            entity.setWindowOpensOn(snap.sustainableLoad().windowOpensOn());
            entity.setWindowClosesOn(snap.sustainableLoad().windowClosesOn());
            entity.setProfileVersion(snap.sustainableLoad().profileVersion());
            entity.setProfileStatus(snap.sustainableLoad().profileStatus());
        }
        entity.setFullBloomOn(snap.fullBloomOn());
        entity.setIssuedAt(snap.issuedAt());
        appendConfirmation(entity, snap.executionConfirmation());

        var roundEntities = new ArrayList<SamplingRoundPersistenceEntity>();
        for (var roundSnap : snap.samplingRounds()) {
            var roundEntity = new SamplingRoundPersistenceEntity();
            roundEntity.setId(UUID.fromString(roundSnap.id().roundId()));
            roundEntity.setPrescription(entity);
            roundEntity.setPlotId(UUID.fromString(snap.plotId().plotId()));
            roundEntity.setActorId(UUID.fromString(roundSnap.actorId().actorId()));
            roundEntity.setClientBatchId(roundSnap.batchId().batchId());
            roundEntity.setIsRepresentative(roundSnap.isRepresentative());
            roundEntity.setCreatedAt(roundSnap.createdAt());

            var recordEntities = new ArrayList<TreeSamplingRecordPersistenceEntity>();
            for (var recordSnap : roundSnap.samplingRecords()) {
                var recordEntity = new TreeSamplingRecordPersistenceEntity();
                recordEntity.setId(UUID.fromString(recordSnap.id().recordId()));
                recordEntity.setRound(roundEntity);
                recordEntity.setTreeTag(recordSnap.treeTag().value());
                recordEntity.setShootCount(recordSnap.shootFruitCount().shootCount());
                recordEntity.setFruitSetCount(recordSnap.shootFruitCount().fruitSetCount());
                recordEntity.setTrunkDiameterMm(
                        recordSnap.trunkCrossSectionalArea() != null ? recordSnap.trunkCrossSectionalArea().trunkDiameterMm() : null
                );
                recordEntity.setSamplingDate(recordSnap.samplingDate());
                recordEntities.add(recordEntity);
            }
            roundEntity.setSamplingRecords(recordEntities);
            roundEntities.add(roundEntity);
        }
        entity.setSamplingRounds(roundEntities);

        return entity;
    }

    /**
     * Updates an existing managed JPA entity with state from the domain aggregate snapshot.
     *
     * @param target the managed JPA entity
     * @param domain the domain aggregate
     */
    public static void updateEntityFromDomain(FruitThinningPrescriptionPersistenceEntity target, FruitThinningPrescription domain) {
        if (target == null || domain == null) {
            return;
        }
        var snap = domain.snapshot();
        target.setStatus(snap.status().name());
        if (snap.sustainableLoad() != null) {
            target.setTargetFruitsPerShoot(snap.sustainableLoad().targetFruitsPerShoot());
            target.setPercentageToRemove(snap.sustainableLoad().percentageToRemove());
            target.setWindowOpensOn(snap.sustainableLoad().windowOpensOn());
            target.setWindowClosesOn(snap.sustainableLoad().windowClosesOn());
            target.setProfileVersion(snap.sustainableLoad().profileVersion());
            target.setProfileStatus(snap.sustainableLoad().profileStatus());
        }
        target.setFullBloomOn(snap.fullBloomOn());
        target.setIssuedAt(snap.issuedAt());
        appendConfirmation(target, snap.executionConfirmation());

        // Synchronize sampling rounds (add newly appended rounds)
        for (var roundSnap : snap.samplingRounds()) {
            UUID roundUuid = UUID.fromString(roundSnap.id().roundId());
            boolean exists = target.getSamplingRounds().stream()
                    .anyMatch(r -> r.getId().equals(roundUuid));
            if (!exists) {
                var roundEntity = new SamplingRoundPersistenceEntity();
                roundEntity.setId(roundUuid);
                roundEntity.setPrescription(target);
                roundEntity.setPlotId(UUID.fromString(snap.plotId().plotId()));
                roundEntity.setActorId(UUID.fromString(roundSnap.actorId().actorId()));
                roundEntity.setClientBatchId(roundSnap.batchId().batchId());
                roundEntity.setIsRepresentative(roundSnap.isRepresentative());
                roundEntity.setCreatedAt(roundSnap.createdAt());

                var recordEntities = new ArrayList<TreeSamplingRecordPersistenceEntity>();
                for (var recordSnap : roundSnap.samplingRecords()) {
                    var recordEntity = new TreeSamplingRecordPersistenceEntity();
                    recordEntity.setId(UUID.fromString(recordSnap.id().recordId()));
                    recordEntity.setRound(roundEntity);
                    recordEntity.setTreeTag(recordSnap.treeTag().value());
                    recordEntity.setShootCount(recordSnap.shootFruitCount().shootCount());
                    recordEntity.setFruitSetCount(recordSnap.shootFruitCount().fruitSetCount());
                    recordEntity.setTrunkDiameterMm(
                            recordSnap.trunkCrossSectionalArea() != null ? recordSnap.trunkCrossSectionalArea().trunkDiameterMm() : null
                    );
                    recordEntity.setSamplingDate(recordSnap.samplingDate());
                    recordEntities.add(recordEntity);
                }
                roundEntity.setSamplingRecords(recordEntities);
                target.getSamplingRounds().add(roundEntity);
            }
        }
    }

    /**
     * Converts a JPA entity into a reconstituted domain aggregate.
     *
     * @param entity the persistent entity
     * @return the reconstituted domain aggregate
     */
    public static FruitThinningPrescription toDomainFromPersistence(FruitThinningPrescriptionPersistenceEntity entity) {
        if (entity == null) {
            return null;
        }

        var roundSnapshots = new ArrayList<SamplingRoundSnapshot>();
        if (entity.getSamplingRounds() != null) {
            for (var roundEntity : entity.getSamplingRounds()) {
                var recordSnapshots = new ArrayList<TreeSamplingRecordSnapshot>();
                if (roundEntity.getSamplingRecords() != null) {
                    for (var recordEntity : roundEntity.getSamplingRecords()) {
                        recordSnapshots.add(new TreeSamplingRecordSnapshot(
                                new SamplingRecordId(recordEntity.getId().toString()),
                                new TreeTag(recordEntity.getTreeTag()),
                                new ShootFruitCount(recordEntity.getShootCount(), recordEntity.getFruitSetCount()),
                                recordEntity.getTrunkDiameterMm() != null ? new TrunkCrossSectionalArea(recordEntity.getTrunkDiameterMm()) : null,
                                recordEntity.getSamplingDate()
                        ));
                    }
                }
                roundSnapshots.add(new SamplingRoundSnapshot(
                        new RoundId(roundEntity.getId().toString()),
                        new UserId(roundEntity.getActorId().toString()),
                        new SamplingBatchId(roundEntity.getClientBatchId()),
                        roundEntity.getIsRepresentative(),
                        recordSnapshots,
                        roundEntity.getCreatedAt()
                ));
            }
        }

        SustainableCropLoad load = (entity.getTargetFruitsPerShoot() != null || entity.getPercentageToRemove() != null)
                ? new SustainableCropLoad(entity.getTargetFruitsPerShoot(), entity.getPercentageToRemove(), entity.getWindowOpensOn(), entity.getWindowClosesOn(),
                        entity.getProfileVersion(), entity.getProfileStatus())
                : SustainableCropLoad.empty();

        var snapshot = new FruitThinningPrescriptionSnapshot(
                new PrescriptionId(entity.getId().toString()),
                new PlotId(entity.getPlotId().toString()),
                new CampaignYear(entity.getCampaignYear()),
                entity.getObservedPlotRevision(),
                PrescriptionStatus.valueOf(entity.getStatus()),
                load,
                entity.getIssuedAt(),
                roundSnapshots,
                confirmationSnapshot(entity.getExecutionConfirmation()),
                entity.getRevision(),
                entity.getFullBloomOn()
        );

        return FruitThinningPrescription.reconstitute(snapshot);
    }

    private static void appendConfirmation(FruitThinningPrescriptionPersistenceEntity parent,
            ExecutionConfirmationSnapshot snapshot) {
        if (snapshot == null || parent.getExecutionConfirmation() != null) {
            return;
        }
        var entity = new ExecutionConfirmationPersistenceEntity();
        entity.setId(UUID.fromString(snapshot.id().confirmationId()));
        entity.setPrescription(parent);
        entity.setExecutionDate(snapshot.executionDate());
        entity.setActualRemovalPercentage(snapshot.actualRemovalPercentage());
        entity.setRemovedKg(snapshot.removedKg());
        entity.setLaborCrewSize(snapshot.laborCrewSize());
        entity.setTimeliness(snapshot.timeliness().name());
        entity.setRecordedAt(snapshot.recordedAt());
        entity.setNotes(snapshot.notes());
        var balance = snapshot.loadBalance();
        if (balance != null) {
            entity.setPreThinningFruitsPerShoot(balance.preThinningFruitsPerShoot());
            entity.setResidualFruitsPerShoot(balance.residualFruitsPerShoot());
            entity.setTargetFruitsPerShoot(balance.targetFruitsPerShoot());
            entity.setDeltaFruitsPerShoot(balance.deltaFruitsPerShoot());
            entity.setLoadRatio(balance.loadRatio());
            entity.setLoadState(balance.loadState().name());
        }
        var projection = snapshot.caliberProjection();
        if (projection != null) {
            entity.setCaliberStatus(projection.status().name());
            entity.setCaliberMostLikelyFruitsPerKg(projection.mostLikelyFruitsPerKg());
            entity.setCaliberFruitsPerKgLow(projection.fruitsPerKgLow());
            entity.setCaliberFruitsPerKgHigh(projection.fruitsPerKgHigh());
            entity.setCaliberConfidenceLevel(projection.confidenceLevel());
            entity.setCaliberCalibrationObservations(projection.calibrationObservations());
            entity.setCaliberModelVersion(projection.modelVersion());
        }
        parent.setExecutionConfirmation(entity);
    }

    private static ExecutionConfirmationSnapshot confirmationSnapshot(ExecutionConfirmationPersistenceEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ExecutionConfirmationSnapshot(new ConfirmationId(entity.getId().toString()),
                entity.getExecutionDate(), entity.getActualRemovalPercentage(), entity.getLaborCrewSize(),
                ExecutionTimeliness.valueOf(entity.getTimeliness()), entity.getRecordedAt(),
                entity.getRemovedKg(), entity.getNotes(), loadBalance(entity), caliberProjection(entity));
    }

    private static LoadBalance loadBalance(ExecutionConfirmationPersistenceEntity entity) {
        if (entity.getLoadState() == null) {
            return null;
        }
        return new LoadBalance(entity.getPreThinningFruitsPerShoot(), entity.getResidualFruitsPerShoot(),
                entity.getTargetFruitsPerShoot(), entity.getDeltaFruitsPerShoot(), entity.getLoadRatio(),
                LoadState.valueOf(entity.getLoadState()));
    }

    private static CaliberProjection caliberProjection(ExecutionConfirmationPersistenceEntity entity) {
        if (entity.getCaliberStatus() == null) {
            return null;
        }
        return new CaliberProjection(CaliberProjectionStatus.valueOf(entity.getCaliberStatus()),
                entity.getCaliberMostLikelyFruitsPerKg(), entity.getCaliberFruitsPerKgLow(),
                entity.getCaliberFruitsPerKgHigh(), entity.getCaliberConfidenceLevel(),
                entity.getCaliberCalibrationObservations(), entity.getCaliberModelVersion());
    }
}
