package com.arcadiadevs.viora.platform.telemetry.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentByIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetAgroclimaticIncidentsQuery;

/**
 * Application query service port handling read-only queries for agroclimatic incidents.
 */
public interface AgroclimaticIncidentQueryService {

    /**
     * Retrieves a summarized list of agroclimatic incidents according to filter parameters.
     *
     * @param query the query specifications
     * @return Result containing {@link AgroclimaticIncidentsSummary} or error
     */
    Result<AgroclimaticIncidentsSummary, ApplicationError> handle(GetAgroclimaticIncidentsQuery query);

    /**
     * Retrieves detailed information of a specific incident including mitigation steps and weekly trend.
     *
     * @param query the incident lookup query
     * @return Result containing {@link AgroclimaticIncidentDetail} or error
     */
    Result<AgroclimaticIncidentDetail, ApplicationError> handle(GetAgroclimaticIncidentByIdQuery query);
}
