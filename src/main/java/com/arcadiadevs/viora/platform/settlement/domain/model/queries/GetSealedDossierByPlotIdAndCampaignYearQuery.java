package com.arcadiadevs.viora.platform.settlement.domain.model.queries;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;

/**
 * Validated question asking for the sealed dossier of one campaign of a plot, with its certification.
 *
 * @param plotId       plot whose dossier is read
 * @param campaignYear campaign to read, 2000 to 2100
 * @param actorId      producer asking for it; only the owner of the plot can read its dossiers
 */
public record GetSealedDossierByPlotIdAndCampaignYearQuery(String plotId, Integer campaignYear, String actorId) {

    public GetSealedDossierByPlotIdAndCampaignYearQuery {
        plotId = new PlotId(plotId).plotId();
        campaignYear = new CampaignYear(campaignYear).value();
        actorId = new UserId(actorId).userId();
    }
}
