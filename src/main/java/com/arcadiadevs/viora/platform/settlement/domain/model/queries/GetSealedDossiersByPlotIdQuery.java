package com.arcadiadevs.viora.platform.settlement.domain.model.queries;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;

/**
 * Validated question asking for every sealed dossier of a plot.
 *
 * @param plotId  plot whose dossiers are listed
 * @param actorId producer asking for them; only the owner of the plot can read its dossiers
 */
public record GetSealedDossiersByPlotIdQuery(String plotId, String actorId) {

    public GetSealedDossiersByPlotIdQuery {
        plotId = new PlotId(plotId).plotId();
        actorId = new UserId(actorId).userId();
    }
}
