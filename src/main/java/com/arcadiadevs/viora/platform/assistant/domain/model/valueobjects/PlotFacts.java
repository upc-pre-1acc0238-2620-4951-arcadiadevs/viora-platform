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
 * @param climate             climate data of the ALERT and CLIMATE screens; {@link ClimateData#NONE} otherwise
 * @param home                data of the HOME screen; {@link HomeData#NONE} otherwise
 */
public record PlotFacts(
        int campaignYear,
        Double bearingIndex,
        int historicalCampaigns,
        boolean caliberEstimated,
        boolean prescriptionIssued,
        Double percentageToRemove,
        int sampledTrees,
        int treesNeeded,
        ClimateData climate,
        HomeData home
) {

    public PlotFacts {
        climate = climate == null ? ClimateData.NONE : climate;
        home = home == null ? HomeData.NONE : home;
    }

    /**
     * Facts of a screen that reads no HOME data.
     */
    public PlotFacts(int campaignYear, Double bearingIndex, int historicalCampaigns, boolean caliberEstimated,
            boolean prescriptionIssued, Double percentageToRemove, int sampledTrees, int treesNeeded,
            ClimateData climate) {
        this(campaignYear, bearingIndex, historicalCampaigns, caliberEstimated, prescriptionIssued,
                percentageToRemove, sampledTrees, treesNeeded, climate, HomeData.NONE);
    }

    /**
     * Facts of a screen that reads no climate data.
     */
    public PlotFacts(int campaignYear, Double bearingIndex, int historicalCampaigns, boolean caliberEstimated,
            boolean prescriptionIssued, Double percentageToRemove, int sampledTrees, int treesNeeded) {
        this(campaignYear, bearingIndex, historicalCampaigns, caliberEstimated, prescriptionIssued,
                percentageToRemove, sampledTrees, treesNeeded, ClimateData.NONE);
    }

    /**
     * Facts of a climate screen (ALERT, CLIMATE): only the climate data matters.
     *
     * @param campaignYear current campaign year
     * @param climate      the climate data of the plot
     * @return the facts
     */
    public static PlotFacts ofClimate(int campaignYear, ClimateData climate) {
        return new PlotFacts(campaignYear, null, 0, false, false, null, 0, 0, climate);
    }

    /**
     * Facts of the HOME screen: only the plot or the active plots of the field matter.
     *
     * @param campaignYear current campaign year
     * @param home         the plot or the active plots of the producer
     * @return the facts
     */
    public static PlotFacts ofHome(int campaignYear, HomeData home) {
        return new PlotFacts(campaignYear, null, 0, false, false, null, 0, 0, ClimateData.NONE, home);
    }
}
