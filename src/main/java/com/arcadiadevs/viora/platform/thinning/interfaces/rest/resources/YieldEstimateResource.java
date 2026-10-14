package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Sustainable and estimated campaign yield of a plot (US26).
 */
@Schema(name = "YieldEstimateResource", description = "Sustainable and estimated campaign yield, in kilograms per hectare")
public record YieldEstimateResource(
        @Schema(description = "AVAILABLE, LOAD_UNKNOWN (only the sustainable yield is known), INSUFFICIENT_HISTORY or AREA_UNKNOWN", example = "AVAILABLE")
        String status,

        @Schema(description = "Mean yield of the earlier harvested campaigns of the plot, in kg/ha; null when unknown", example = "8200.0")
        Double sustainableKgPerHa,

        @Schema(description = "Yield expected with the current crop load, in kg/ha; null when unknown", example = "11685.0")
        Double estimatedKgPerHa,

        @Schema(description = "CALIBER_ADJUSTED (fruit size learned from real harvests) or FRUIT_COUNT_ONLY (follows the fruit count; above the target it is a ceiling); null without estimate", example = "FRUIT_COUNT_ONLY")
        String basis,

        @Schema(description = "Earlier harvested campaigns the sustainable yield comes from", example = "3")
        Integer harvestCampaigns,

        @Schema(description = "Earlier harvested campaigns needed", example = "3")
        Integer requiredHarvestCampaigns
) {
}
