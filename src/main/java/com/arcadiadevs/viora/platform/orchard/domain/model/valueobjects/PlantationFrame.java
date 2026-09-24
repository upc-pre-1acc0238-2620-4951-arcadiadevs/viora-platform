package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

/**
 * Value Object defining the spatial grid of the olive orchard planting frame (row x tree spacing).
 * Invariant: Spacings must be strictly positive numbers.
 *
 * @param rowSpacingM  distance between rows in meters
 * @param treeSpacingM distance between trees within the row in meters
 */
public record PlantationFrame(Double rowSpacingM, Double treeSpacingM) {

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
    }
}
