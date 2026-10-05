package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Crop load left on the trees after thinning compared with the sustainable target. */
@Schema(description = "Load balance: residual = preThinning x (1 - actualRemovalPercentage/100); loadRatio = residual / target")
public record LoadBalanceResource(
        @Schema(description = "Mean fruits per shoot measured by sampling before thinning", example = "42.0")
        Double preThinningFruitsPerShoot,
        @Schema(description = "Estimated fruits per shoot left after the actual removal", example = "31.5")
        Double residualFruitsPerShoot,
        @Schema(description = "Sustainable target fruits per shoot of the prescription", example = "30.0")
        Double targetFruitsPerShoot,
        @Schema(description = "Residual minus target; negative means below the target", example = "1.5")
        Double deltaFruitsPerShoot,
        @Schema(description = "Residual divided by target", example = "1.05")
        Double loadRatio,
        @Schema(description = "BALANCED (ratio <= 1.0), MODERATE_OVERLOAD (<= 1.3) or SEVERE_OVERLOAD (> 1.3)",
                allowableValues = {"BALANCED", "MODERATE_OVERLOAD", "SEVERE_OVERLOAD"})
        String loadState) {
}
