package com.arcadiadevs.viora.platform.settlement.application.queryservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementByPlotIdAndCampaignYearQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementsByPlotIdQuery;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

import java.util.List;

/** Use case: read the settlements of a plot, of every campaign or of one. */
public interface HarvestSettlementQueryService {

    /**
     * @param query the validated question
     * @return {@link Result} with the settlements of the plot, newest campaign first, or an
     *         {@link ApplicationError} when the plot is unknown or belongs to another producer
     */
    Result<List<HarvestSettlementSnapshot>, ApplicationError> handle(GetHarvestSettlementsByPlotIdQuery query);

    /**
     * @param query the validated question
     * @return {@link Result} with the settlement of the campaign, or an {@link ApplicationError} when the plot is
     *         unknown, belongs to another producer or has not settled that campaign
     */
    Result<HarvestSettlementSnapshot, ApplicationError> handle(
            GetHarvestSettlementByPlotIdAndCampaignYearQuery query);
}
