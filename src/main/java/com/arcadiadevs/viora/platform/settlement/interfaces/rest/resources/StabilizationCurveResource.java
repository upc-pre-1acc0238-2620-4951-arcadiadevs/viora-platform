package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Interannual stabilization of the plot as it stood when the campaign was settled. */
@Schema(description = "Hoblyn alternation index over consecutive campaigns: baseline = Phenology history before the "
        + "first settlement, managed = settled campaigns. ARR = (baseline - managed) / baseline; target ARR >= 0.30. "
        + "Each index needs two pairs of consecutive campaigns.")
public record StabilizationCurveResource(
        @Schema(allowableValues = {"EVALUATED", "INSUFFICIENT_BASELINE", "INSUFFICIENT_SETTLEMENTS",
                "NO_BASELINE_ALTERNATION"}) String status,
        Integer baselineCampaigns,
        Integer settledCampaigns,
        @Schema(description = "Mean yield of the baseline campaigns, kg", nullable = true) Double baselineYieldKg,
        @Schema(nullable = true) Double baselineAlternationIndex,
        @Schema(nullable = true) Double managedAlternationIndex,
        @Schema(description = "Amplitude reduction rate as a fraction; only when EVALUATED", nullable = true)
        Double amplitudeReductionRate,
        @Schema(description = "ARR >= 0.30; only when EVALUATED", nullable = true) Boolean targetAchieved,
        @Schema(description = "Sample variance of settled yields, kg^2", nullable = true) Double interannualVarianceKg2,
        @Schema(description = "Sample standard deviation / mean of settled yields", nullable = true)
        Double coefficientOfVariation,
        @Schema(description = "Pairs of consecutive campaigns needed by each index", example = "2")
        Integer requiredConsecutivePairs) {
}
