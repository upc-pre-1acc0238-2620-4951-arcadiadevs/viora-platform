package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The projected intake of one caliber range of the plant.
 *
 * @param minFruitsPerKg   the biggest fruit of the range
 * @param maxFruitsPerKg   the smallest fruit of the range
 * @param tons             the tons of the range
 * @param greenTons        green olives, by each plot's history
 * @param blackTons        black olives
 * @param unclassifiedTons olives of plots without history
 */
@Schema(name = "CaliberIntakeResource", description = "Projected intake of one caliber range")
public record CaliberIntakeResource(
        @Schema(example = "140") int minFruitsPerKg,
        @Schema(example = "180") int maxFruitsPerKg,
        @Schema(example = "320.0") double tons,
        @Schema(example = "190.0") double greenTons,
        @Schema(example = "110.0") double blackTons,
        @Schema(example = "20.0") double unclassifiedTons
) {
}
