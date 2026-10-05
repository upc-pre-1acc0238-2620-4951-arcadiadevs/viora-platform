package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Immutable value object representing statistical field sampling coverage.
 *
 * @param plotId             evaluated plot identifier
 * @param campaignYear       evaluated agricultural campaign
 * @param sampledTreesCount  unique evaluated trees across all sampling rounds
 * @param sampledShootsCount total evaluated shoots across all sampling rounds
 * @param meanFruitsPerShoot mean fruits per sampled shoot
 * @param isRepresentative   whether the minimum tree coverage threshold is reached
 * @param treesNeeded        number of additional unique trees required for representativeness
 */
public record SamplingStatisticalSummary(
        PlotId plotId,
        CampaignYear campaignYear,
        int sampledTreesCount,
        int sampledShootsCount,
        double meanFruitsPerShoot,
        boolean isRepresentative,
        int treesNeeded
) {

    /**
     * Compact constructor enforcing non-negative statistical values.
     *
     * @param plotId             evaluated plot identifier
     * @param campaignYear       evaluated agricultural campaign
     * @param sampledTreesCount  unique evaluated trees across all sampling rounds
     * @param sampledShootsCount total evaluated shoots across all sampling rounds
     * @param meanFruitsPerShoot mean fruits per sampled shoot
     * @param isRepresentative   whether the minimum tree coverage threshold is reached
     * @param treesNeeded        number of additional unique trees required
     */
    public SamplingStatisticalSummary {
        if (plotId == null) {
            throw new IllegalArgumentException("thinning.plot.id.null_or_empty");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("thinning.campaign_year.null");
        }
        if (sampledTreesCount < 0) {
            throw new IllegalArgumentException("thinning.sampled_trees.negative");
        }
        if (sampledShootsCount < 0) {
            throw new IllegalArgumentException("thinning.sampled_shoots.negative");
        }
        if (!Double.isFinite(meanFruitsPerShoot) || meanFruitsPerShoot < 0.0) {
            throw new IllegalArgumentException("thinning.mean_fruits_per_shoot.invalid");
        }
        if (treesNeeded < 0) {
            throw new IllegalArgumentException("thinning.trees_needed.negative");
        }
    }
}
