package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * Data of a plot the catalog needs to decide which questions can be answered. Every figure is already computed
 * by the context that owns it.
 *
 * @param campaignYear        current campaign year
 * @param bearingIndex        latest Biennial Bearing Index; null when it cannot be calculated yet
 * @param historicalCampaigns campaigns with a registered harvest
 * @param caliberEstimated    whether the caliber projection of the current campaign is estimated
 * @param prescriptionIssued  whether the thinning prescription of the current campaign was issued
 * @param percentageToRemove  prescribed percentage of fruits to remove; null while not issued
 * @param sampledTrees        distinct trees sampled in the current campaign
 * @param treesNeeded         trees still missing for a representative sampling
 */
public record PlotFacts(
        int campaignYear,
        Double bearingIndex,
        int historicalCampaigns,
        boolean caliberEstimated,
        boolean prescriptionIssued,
        Double percentageToRemove,
        int sampledTrees,
        int treesNeeded
) {
}
