package com.arcadiadevs.viora.platform.territory.application.queryservices;

/**
 * Read model of the projected intake of one caliber range.
 *
 * @param minFruitsPerKg   the biggest fruit of the range
 * @param maxFruitsPerKg   the smallest fruit of the range
 * @param tons             the tons of the range
 * @param greenTons        green olives, by each plot's history
 * @param blackTons        black olives
 * @param unclassifiedTons olives of plots without history
 */
public record CaliberIntakeView(int minFruitsPerKg, int maxFruitsPerKg, double tons, double greenTons,
                                double blackTons, double unclassifiedTons) {
}
