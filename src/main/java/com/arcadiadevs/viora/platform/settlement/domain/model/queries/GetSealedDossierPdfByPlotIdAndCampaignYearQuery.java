package com.arcadiadevs.viora.platform.settlement.domain.model.queries;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;

/**
 * Validated question asking for the exact stored PDF of the sealed dossier of one campaign of a plot.
 *
 * @param plotId       plot whose dossier is downloaded
 * @param campaignYear campaign to download, 2000 to 2100
 * @param actorId      producer asking for it; only the owner of the plot can read its dossiers
 */
public record GetSealedDossierPdfByPlotIdAndCampaignYearQuery(String plotId, Integer campaignYear, String actorId) {

    public GetSealedDossierPdfByPlotIdAndCampaignYearQuery {
        plotId = new PlotId(plotId).plotId();
        campaignYear = new CampaignYear(campaignYear).value();
        actorId = new UserId(actorId).userId();
    }
}
