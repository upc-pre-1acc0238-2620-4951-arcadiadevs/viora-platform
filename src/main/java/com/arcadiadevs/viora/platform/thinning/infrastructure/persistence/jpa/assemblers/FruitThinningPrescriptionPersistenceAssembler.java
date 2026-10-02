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
            entity.setTargetFruitsPerMeter(snap.sustainableLoad().targetFruitsPerMeter());
            entity.setPercentageToRemove(snap.sustainableLoad().percentageToRemove());
            entity.setWindowClosesOn(snap.sustainableLoad().windowClosesOn());
        }
        entity.setIssuedAt(snap.issuedAt());

        var roundEntities = new ArrayList<SamplingRoundPersistenceEntity>();
        for (var roundSnap : snap.samplingRounds()) {
            var roundEntity = new SamplingRoundPersistenceEntity();
            roundEntity.setId(UUID.fromString(roundSnap.id().roundId()));
            roundEntity.setPrescription(entity);
            roundEntity.setPlotId(UUID.fromString(snap.plotId().plotId()));
            roundEntity.setActorId(UUID.fromString(roundSnap.actorId().actorId()));
            roundEntity.setClientBatchId(roundSnap.batchId().batchId());
            roundEntity.setIsRepresentative(roundSnap.isRepresentative());

            var recordEntities = new ArrayList<TreeSamplingRecordPersistenceEntity>();
            for (var recordSnap : roundSnap.samplingRecords()) {
                var recordEntity = new TreeSamplingRecordPersistenceEntity();
                recordEntity.setId(UUID.fromString(recordSnap.id().recordId()));
                recordEntity.setRound(roundEntity);
                recordEntity.setTreeTag(recordSnap.treeTag().value());
                recordEntity.setShootCount(recordSnap.shootFruitCount().shootCount());
                recordEntity.setFruitSetCount(recordSnap.shootFruitCount().fruitSetCount());
                recordEntity.setTrunkDiameterMm(recordSnap.trunkCrossSectionalArea().trunkDiameterMm());
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
            target.setTargetFruitsPerMeter(snap.sustainableLoad().targetFruitsPerMeter());
            target.setPercentageToRemove(snap.sustainableLoad().percentageToRemove());
            target.setWindowClosesOn(snap.sustainableLoad().windowClosesOn());
        }
        target.setIssuedAt(snap.issuedAt());

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

                var recordEntities = new ArrayList<TreeSamplingRecordPersistenceEntity>();
                for (var recordSnap : roundSnap.samplingRecords()) {
                    var recordEntity = new TreeSamplingRecordPersistenceEntity();
                    recordEntity.setId(UUID.fromString(recordSnap.id().recordId()));
                    recordEntity.setRound(roundEntity);
                    recordEntity.setTreeTag(recordSnap.treeTag().value());
                    recordEntity.setShootCount(recordSnap.shootFruitCount().shootCount());
                    recordEntity.setFruitSetCount(recordSnap.shootFruitCount().fruitSetCount());
                    recordEntity.setTrunkDiameterMm(recordSnap.trunkCrossSectionalArea().trunkDiameterMm());
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
                                new TrunkCrossSectionalArea(recordEntity.getTrunkDiameterMm()),
                                recordEntity.getSamplingDate()
                        ));
                    }
                }
                roundSnapshots.add(new SamplingRoundSnapshot(
                        new RoundId(roundEntity.getId().toString()),
                        new UserId(roundEntity.getActorId().toString()),
                        new SamplingBatchId(roundEntity.getClientBatchId()),
                        roundEntity.getIsRepresentative(),
                        recordSnapshots
                ));
            }
        }

        SustainableCropLoad load = (entity.getTargetFruitsPerMeter() != null || entity.getPercentageToRemove() != null)
                ? new SustainableCropLoad(entity.getTargetFruitsPerMeter(), entity.getPercentageToRemove(), entity.getWindowClosesOn())
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
                null,
                entity.getRevision()
        );

        return FruitThinningPrescription.reconstitute(snapshot);
    }
}
