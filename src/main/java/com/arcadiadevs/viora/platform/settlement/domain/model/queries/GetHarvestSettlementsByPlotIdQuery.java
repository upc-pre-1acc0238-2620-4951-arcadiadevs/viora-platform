package com.arcadiadevs.viora.platform.settlement.domain.model.queries;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

/**
 * Validated question asking for every settlement of a plot.
 *
 * @param plotId  plot whose settlements are read
 * @param actorId producer asking for them
 */
public record GetHarvestSettlementsByPlotIdQuery(String plotId, String actorId) {

    public GetHarvestSettlementsByPlotIdQuery {
        plotId = new PlotId(plotId).plotId();
        actorId = new UserId(actorId).userId();
    }
}
