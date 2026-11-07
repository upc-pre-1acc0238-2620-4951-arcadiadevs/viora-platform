package com.arcadiadevs.viora.platform.territory.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetIntakeForecastByCampaignYearAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetIntakeForecastsByManagerIdQuery;

import java.util.List;

/**
 * Query service port of the intake of the manager's license.
 */
public interface IntakeForecastQueryService {

    /**
     * Lists the campaigns the manager can see, newest first: the one in progress, projected, and the past ones with
     * harvests settled since their members joined.
     *
     * @param query the manager
     * @return the intake of each campaign, without the sectors, or a 403
     */
    Result<List<IntakeForecastView>, ApplicationError> handle(GetIntakeForecastsByManagerIdQuery query);

    /**
     * Reads the intake of one campaign with its sectors.
     *
     * @param query the campaign and the manager
     * @return the intake, a 404 for a future campaign, or a 403
     */
    Result<IntakeForecastView, ApplicationError> handle(GetIntakeForecastByCampaignYearAndManagerIdQuery query);
}
