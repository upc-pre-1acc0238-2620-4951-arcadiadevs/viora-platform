package com.arcadiadevs.viora.platform.thinning.domain.model.entities;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.TreeSamplingRecordSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingRecordId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ShootFruitCount;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.TreeTag;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.TrunkCrossSectionalArea;

import java.time.LocalDate;

/**
 * Domain entity representing an individual olive tree evaluated during a field sampling round.
 */
public class TreeSamplingRecord {

    private final SamplingRecordId id;
    private final TreeTag treeTag;
    private final ShootFruitCount shootFruitCount;
    private final TrunkCrossSectionalArea trunkCrossSectionalArea;
    private final LocalDate samplingDate;

    /**
     * Constructs a new TreeSamplingRecord entity.
     *
     * @param id                      unique record identifier
     * @param treeTag                 tree tag identification
     * @param shootFruitCount         counts of shoots and set fruits
     * @param trunkCrossSectionalArea trunk diameter measurement
     * @param samplingDate            date when tree was sampled
     */
    public TreeSamplingRecord(
            SamplingRecordId id,
            TreeTag treeTag,
            ShootFruitCount shootFruitCount,
            TrunkCrossSectionalArea trunkCrossSectionalArea,
            LocalDate samplingDate
    ) {
        if (samplingDate == null) {
            throw new IllegalArgumentException("thinning.sampling_date.null");
        }
        if (samplingDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("thinning.sampling_date.future");
        }
        this.id = id;
        this.treeTag = treeTag;
        this.shootFruitCount = shootFruitCount;
        this.trunkCrossSectionalArea = trunkCrossSectionalArea;
        this.samplingDate = samplingDate;
    }

    /**
     * Factory method creating a new TreeSamplingRecord with generated ID.
     *
     * @param treeTag                 tree tag identification
     * @param shootCount              number of counted shoots
     * @param fruitSetCount           number of counted fruits
     * @param trunkDiameterMm         trunk diameter in mm
     * @param samplingDate            date when tree was sampled
     * @return a consistent TreeSamplingRecord
     */
    public static TreeSamplingRecord create(
            String treeTag,
            Integer shootCount,
            Integer fruitSetCount,
            Double trunkDiameterMm,
            LocalDate samplingDate
    ) {
        return new TreeSamplingRecord(
                new SamplingRecordId(),
                new TreeTag(treeTag),
                new ShootFruitCount(shootCount, fruitSetCount),
                trunkDiameterMm != null ? new TrunkCrossSectionalArea(trunkDiameterMm) : null,
                samplingDate
        );
    }

    /**
     * Reconstitutes an entity from snapshot.
     *
     * @param snapshot the snapshot to restore from
     * @return reconstituted TreeSamplingRecord
     */
    public static TreeSamplingRecord fromSnapshot(TreeSamplingRecordSnapshot snapshot) {
        return new TreeSamplingRecord(
                snapshot.id(),
                snapshot.treeTag(),
                snapshot.shootFruitCount(),
                snapshot.trunkCrossSectionalArea(),
                snapshot.samplingDate()
        );
    }

    /**
     * Creates an immutable snapshot of this tree sampling record.
     *
     * @return snapshot representation
     */
    public TreeSamplingRecordSnapshot snapshot() {
        return new TreeSamplingRecordSnapshot(
                id,
                treeTag,
                shootFruitCount,
                trunkCrossSectionalArea,
                samplingDate
        );
    }

    public SamplingRecordId id() {
        return id;
    }

    public TreeTag treeTag() {
        return treeTag;
    }

    public ShootFruitCount shootFruitCount() {
        return shootFruitCount;
    }

    public TrunkCrossSectionalArea trunkCrossSectionalArea() {
        return trunkCrossSectionalArea;
    }

    public LocalDate samplingDate() {
        return samplingDate;
    }
}
