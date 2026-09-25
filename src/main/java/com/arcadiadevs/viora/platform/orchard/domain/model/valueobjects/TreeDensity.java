package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

/**
 * Value Object representing the tree density (number of olive trees per hectare).
 * Invariant: Must be >= 50 trees per hectare.
 * Formula: D = 10000 / (rowSpacingM * treeSpacingM)
 *
 * @param treesPerHectare density in trees per hectare
 */
public record TreeDensity(Integer treesPerHectare) {

    /**
     * Compact constructor validating minimum density threshold.
     *
     * @param treesPerHectare the density count
     */
    public TreeDensity {
        if (treesPerHectare == null || treesPerHectare < 50) {
            throw new IllegalArgumentException("plot.density.min");
        }
    }

    /**
     * Calculates the tree density from a given plantation frame.
     *
     * @param frame the plantation grid dimensions
     * @return a calculated TreeDensity instance
     */
    public static TreeDensity from(PlantationFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("plot.frame.null");
        }
        double areaPerTree = frame.rowSpacingM() * frame.treeSpacingM();
        int calculated = (int) Math.round(10000.0 / areaPerTree);
        return new TreeDensity(calculated);
    }
}
