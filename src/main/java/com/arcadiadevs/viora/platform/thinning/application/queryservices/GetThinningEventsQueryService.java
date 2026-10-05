package com.arcadiadevs.viora.platform.thinning.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningEventsQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ThinningEvent;

import java.util.List;

/**
 * Application query service port for retrieving thinning milestone events for the agronomic logbook.
 */
public interface GetThinningEventsQueryService {

    /**
     * Handles the retrieval of thinning events based on campaign year, actor, and optional plot filter.
     *
     * @param query the lookup query specification
     * @return result containing a list of thinning events or an application error
     */
    Result<List<ThinningEvent>, ApplicationError> handle(GetThinningEventsQuery query);
}
