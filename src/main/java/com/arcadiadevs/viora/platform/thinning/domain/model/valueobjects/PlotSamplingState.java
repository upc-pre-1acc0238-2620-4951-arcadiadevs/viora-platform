package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Immutable domain value object representing the field sampling progress and coverage state of an olive plot.
 *
 * @param plotId            plot identifier
 * @param plotName          human-readable plot name
 * @param variety           botanical olive variety
 * @param areaHectares      plot surface area in hectares
 * @param campaignYear      agricultural campaign year
 * @param samplingStatus    sampling progress state (NOT_STARTED, IN_PROGRESS, COMPLETED)
 * @param sampledTreesCount unique evaluated trees count
 * @param treesNeeded       additional trees needed to achieve representativeness
 * @param isRepresentative  whether representativeness threshold is satisfied
 */
public record PlotSamplingState(
        PlotId plotId,
        String plotName,
        String variety,
        Double areaHectares,
        CampaignYear campaignYear,
        String samplingStatus,
        int sampledTreesCount,
        int treesNeeded,
        boolean isRepresentative
) {

    /**
     * Compact constructor validating required domain fields.
     */
    public PlotSamplingState {
        if (plotId == null) {
            throw new IllegalArgumentException("thinning.plot.id.null_or_empty");
        }
        if (plotName == null || plotName.isBlank()) {
            throw new IllegalArgumentException("thinning.plot.name.null_or_empty");
        }
        if (variety == null || variety.isBlank()) {
            throw new IllegalArgumentException("thinning.plot.variety.null_or_empty");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("thinning.campaign_year.null");
        }
        if (samplingStatus == null || samplingStatus.isBlank()) {
            throw new IllegalArgumentException("thinning.sampling_status.null_or_empty");
        }
        if (sampledTreesCount < 0) {
            throw new IllegalArgumentException("thinning.sampled_trees.negative");
        }
        if (treesNeeded < 0) {
            throw new IllegalArgumentException("thinning.trees_needed.negative");
        }
    }
}
