package com.arcadiadevs.viora.platform.thinning.domain.model.queries;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;

import java.time.Year;
import java.time.ZoneOffset;

/**
 * Domain query for retrieving the sampling coverage summary of a plot and campaign.
 *
 * @param plotId       target plot identifier
 * @param campaignYear agricultural campaign year
 */
public record GetSamplingSummaryByPlotIdQuery(
        PlotId plotId,
        CampaignYear campaignYear
) {

    /**
     * Compact constructor enforcing the required plot reference.
     *
     * @param plotId       target plot identifier
     * @param campaignYear agricultural campaign year
     */
    public GetSamplingSummaryByPlotIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("thinning.plot.id.null_or_empty");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("thinning.campaign_year.null");
        }
    }

    /**
     * Convenience constructor using the current UTC calendar year.
     *
     * @param plotId target plot identifier
     */
    public GetSamplingSummaryByPlotIdQuery(PlotId plotId) {
        this(plotId, new CampaignYear(Year.now(ZoneOffset.UTC).getValue()));
    }
}
