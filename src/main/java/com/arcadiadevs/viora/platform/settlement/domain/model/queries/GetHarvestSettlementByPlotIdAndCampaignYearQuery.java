package com.arcadiadevs.viora.platform.settlement.domain.model.queries;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

/**
 * Validated question asking for the settlement of one campaign of a plot.
 *
 * @param plotId       plot whose settlement is read
 * @param campaignYear campaign to read, 2000 to 2100
 * @param actorId      producer asking for it
 */
public record GetHarvestSettlementByPlotIdAndCampaignYearQuery(String plotId, Integer campaignYear, String actorId) {

    public GetHarvestSettlementByPlotIdAndCampaignYearQuery {
        plotId = new PlotId(plotId).plotId();
        campaignYear = new CampaignYear(campaignYear).value();
        actorId = new UserId(actorId).userId();
    }
}
