package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Crop load measured by the sampling, before thinning, against the sustainable target (US26).
 */
@Schema(name = "CurrentLoadResource", description = "Crop load before thinning against the sustainable target")
public record CurrentLoadResource(
        @Schema(description = "Mean fruits per sampled shoot of the representative sampling", example = "0.57")
        Double fruitsPerShoot,

        @Schema(description = "Sustainable target in fruits per shoot, from the prescription or the variety profile", example = "0.4")
        Double targetFruitsPerShoot,

        @Schema(description = "Current load divided by the target", example = "1.425")
        Double loadRatio,

        @Schema(description = "BALANCED (ratio up to 1.0), MODERATE_OVERLOAD (up to 1.3) or SEVERE_OVERLOAD (above 1.3)", example = "SEVERE_OVERLOAD")
        String loadState
) {
}
