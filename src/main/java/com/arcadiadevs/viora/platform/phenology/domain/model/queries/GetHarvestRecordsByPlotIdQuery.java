package com.arcadiadevs.viora.platform.phenology.domain.model.queries;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;

/**
 * Domain query record for retrieving historical harvest records of an orchard plot,
 * with optional campaign year filtering.
 *
 * @param plotId       the logical reference to the orchard plot
 * @param campaignYear the optional campaign year filter
 */
public record GetHarvestRecordsByPlotIdQuery(
        PlotId plotId,
        CampaignYear campaignYear
) {

    /**
     * Compact constructor validating that the target plotId is non-null.
     *
     * @param plotId       the logical plot identifier
     * @param campaignYear optional campaign year
     * @throws IllegalArgumentException if plotId is null
     */
    public GetHarvestRecordsByPlotIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor mapping raw String plotId and nullable Integer campaignYear into typed Value Objects.
     *
     * @param plotId       the raw plot UUID string
     * @param campaignYear optional raw integer campaign year
     */
    public GetHarvestRecordsByPlotIdQuery(String plotId, Integer campaignYear) {
        this(
                new PlotId(plotId),
                campaignYear != null ? new CampaignYear(campaignYear) : null
        );
    }
}
