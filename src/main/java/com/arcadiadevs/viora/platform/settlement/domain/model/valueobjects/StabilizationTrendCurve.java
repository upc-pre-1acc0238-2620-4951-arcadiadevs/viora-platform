package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Interannual stabilization of a plot, frozen in each settlement as it stood when the campaign was closed.
 *
 * @param status                    curve outcome
 * @param baselineCampaigns         historical campaigns before the first settlement
 * @param settledCampaigns          settled campaigns including the current one
 * @param baselineYieldKg           mean yield of the baseline campaigns, or {@code null} without baseline
 * @param baselineAlternationIndex  Hoblyn index of the baseline, or {@code null} when insufficient
 * @param managedAlternationIndex   Hoblyn index of the settled campaigns, or {@code null} when insufficient
 * @param amplitudeReductionRate    {@code (baseline - managed) / baseline}, only when {@code EVALUATED}
 * @param targetAchieved            {@code amplitudeReductionRate >= 0.30}, only when {@code EVALUATED}
 * @param interannualVarianceKg2    sample variance of settled yields in kg^2, from two settlements on
 * @param coefficientOfVariation    sample standard deviation divided by mean settled yield, from two settlements on
 */
public record StabilizationTrendCurve(
        StabilizationStatus status,
        int baselineCampaigns,
        int settledCampaigns,
        Double baselineYieldKg,
        Double baselineAlternationIndex,
        Double managedAlternationIndex,
        Double amplitudeReductionRate,
        Boolean targetAchieved,
        Double interannualVarianceKg2,
        Double coefficientOfVariation
) {

    public StabilizationTrendCurve {
        if (status == null || baselineCampaigns < 0 || settledCampaigns < 0) {
            throw new IllegalArgumentException("settlement.curve.invalid");
        }
        boolean evaluated = status == StabilizationStatus.EVALUATED;
        if (evaluated != (amplitudeReductionRate != null) || evaluated != (targetAchieved != null)) {
            throw new IllegalArgumentException("settlement.curve.invalid");
        }
    }
}
