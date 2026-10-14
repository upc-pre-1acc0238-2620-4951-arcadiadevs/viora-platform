package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Sustainable and estimated campaign yield of a plot, in kilograms per hectare (US26).
 *
 * @param status                 whether the figures could be computed and, if not, what is missing
 * @param sustainableKgPerHa   mean yield of the earlier harvested campaigns; null when unknown
 * @param estimatedKgPerHa     yield expected with the current crop load; null when unknown
 * @param basis                  how the estimate accounts for fruit size; null when there is no estimate
 * @param harvestCampaigns       number of earlier harvested campaigns the sustainable yield comes from
 * @param caliberElasticity      the {@code beta} used for fruit size; 0 when the yield follows the fruit count alone
 */
public record YieldEstimate(
        YieldEstimateStatus status,
        Double sustainableKgPerHa,
        Double estimatedKgPerHa,
        YieldEstimateBasis basis,
        int harvestCampaigns,
        double caliberElasticity
) {

    public YieldEstimate {
        if (status == null) {
            throw new IllegalArgumentException("thinning.yield.status.null");
        }
        if (harvestCampaigns < 0) {
            throw new IllegalArgumentException("thinning.yield.campaigns.negative");
        }
        if (status == YieldEstimateStatus.AVAILABLE && (estimatedKgPerHa == null || basis == null)) {
            throw new IllegalArgumentException("thinning.yield.estimate.missing");
        }
    }

    /**
     * Creates an estimate that could not be computed.
     *
     * @param status           why it is missing
     * @param harvestCampaigns earlier harvested campaigns found
     * @return an estimate without figures
     */
    public static YieldEstimate unavailable(YieldEstimateStatus status, int harvestCampaigns) {
        return new YieldEstimate(status, null, null, null, harvestCampaigns, 0.0);
    }
}
