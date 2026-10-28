package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

/**
 * Value Object defining the spatial grid of the olive orchard planting frame (row x tree spacing).
 * Invariant: Spacings must be strictly positive numbers.
 *
 * @param rowSpacingM  distance between rows in meters
 * @param treeSpacingM distance between trees within the row in meters
 */
public record PlantationFrame(Double rowSpacingM, Double treeSpacingM) {

    /** Closest spacing accepted, in metres; even super-intensive hedgerows plant trees further apart. */
    public static final double MIN_SPACING_M = 0.5;

    /** Widest spacing accepted, in metres; far above any traditional olive frame. */
    public static final double MAX_SPACING_M = 50.0;

    /**
     * Compact constructor validating positive planting spacing dimensions.
     *
     * @param rowSpacingM  row spacing in meters
     * @param treeSpacingM tree spacing in meters
     */
    public PlantationFrame {
        if (rowSpacingM == null || rowSpacingM <= 0.0 || treeSpacingM == null || treeSpacingM <= 0.0) {
            throw new IllegalArgumentException("plot.spacing.positive");
        }
        if (!Double.isFinite(rowSpacingM) || !Double.isFinite(treeSpacingM)
                || rowSpacingM < MIN_SPACING_M || treeSpacingM < MIN_SPACING_M
                || rowSpacingM > MAX_SPACING_M || treeSpacingM > MAX_SPACING_M) {
            throw new IllegalArgumentException("plot.spacing.out_of_range");
        }
    }
}
