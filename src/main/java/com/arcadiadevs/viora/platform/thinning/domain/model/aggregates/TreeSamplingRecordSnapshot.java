package com.arcadiadevs.viora.platform.thinning.domain.model.aggregates;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingRecordId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ShootFruitCount;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.TreeTag;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.TrunkCrossSectionalArea;

import java.time.LocalDate;

/**
 * Immutable snapshot representing the state of an individual tree sampling record.
 *
 * @param id                      unique identifier of the record
 * @param treeTag                 physical tag identifier of the tree
 * @param shootFruitCount         counts of shoots and set fruits value object
 * @param trunkCrossSectionalArea trunk measurement value object
 * @param samplingDate            date when field sampling was conducted
 */
public record TreeSamplingRecordSnapshot(
        SamplingRecordId id,
        TreeTag treeTag,
        ShootFruitCount shootFruitCount,
        TrunkCrossSectionalArea trunkCrossSectionalArea,
        LocalDate samplingDate
) {
}

