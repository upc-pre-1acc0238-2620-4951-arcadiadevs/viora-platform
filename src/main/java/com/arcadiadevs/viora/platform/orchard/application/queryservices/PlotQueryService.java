package com.arcadiadevs.viora.platform.orchard.application.queryservices;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetAllActivePlotsByProducerIdQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery;

import java.util.List;

/**
 * Application query service port for querying orchard plots.
 * Exposes explicit query handlers without conditional branching or optional/null parameters.
 */
public interface PlotQueryService {

    /**
     * Retrieves all active orchard plots belonging to the specified producer.
     *
     * @param query the query containing the producer identifier
     * @return list of active plot aggregate roots
     */
    List<Plot> handle(GetAllActivePlotsByProducerIdQuery query);

    /**
     * Retrieves orchard plots modified at or after a specific timestamp for delta synchronization.
     *
     * @param query the query containing producer identifier and lower-bound timestamp
     * @return list of plot aggregate roots modified since the timestamp
     */
    List<Plot> handle(GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery query);
}
