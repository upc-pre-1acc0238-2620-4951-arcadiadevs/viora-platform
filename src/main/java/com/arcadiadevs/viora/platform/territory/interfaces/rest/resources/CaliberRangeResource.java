package com.arcadiadevs.viora.platform.territory.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * A caliber range of the cooperative's plant, in fruits per kilogram; both ends belong to it.
 *
 * @param minFruitsPerKg the biggest fruit of the class
 * @param maxFruitsPerKg the smallest fruit of the class
 */
@Schema(name = "CaliberRangeResource", description = "Caliber range of the plant, in fruits per kilogram")
public record CaliberRangeResource(
        @NotNull(message = "territory.caliber_range.incomplete")
        @Schema(example = "140", requiredMode = Schema.RequiredMode.REQUIRED) Integer minFruitsPerKg,
        @NotNull(message = "territory.caliber_range.incomplete")
        @Schema(example = "180", requiredMode = Schema.RequiredMode.REQUIRED) Integer maxFruitsPerKg
) {
}
