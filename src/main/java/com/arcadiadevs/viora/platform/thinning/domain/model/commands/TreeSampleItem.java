package com.arcadiadevs.viora.platform.thinning.domain.model.commands;

import java.time.LocalDate;

/**
 * Command element representing a single tree sampling record within a batch.
 *
 * @param treeTag         physical tag of the tree
 * @param shootCount      number of evaluated shoots
 * @param fruitSetCount   number of observed fruits
 * @param trunkDiameterMm trunk diameter measurement in mm
 * @param samplingDate    date when sample was taken
 */
public record TreeSampleItem(
        String treeTag,
        Integer shootCount,
        Integer fruitSetCount,
        Double trunkDiameterMm,
        LocalDate samplingDate
) {

    /**
     * Compact constructor validating strictly non-null arguments.
     */
    public TreeSampleItem {
        if (treeTag == null) {
            throw new IllegalArgumentException("thinning.tree_tag.null_or_empty");
        }
        if (shootCount == null) {
            throw new IllegalArgumentException("thinning.shoot_count.positive");
        }
        if (fruitSetCount == null) {
            throw new IllegalArgumentException("thinning.fruit_count.negative");
        }
        if (samplingDate == null) {
            throw new IllegalArgumentException("thinning.sampling_date.null");
        }
    }
}
