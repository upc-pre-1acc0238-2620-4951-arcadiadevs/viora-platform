package com.arcadiadevs.viora.platform.orchard.domain.model.queries;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotStatus;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;

/**
 * Query to retrieve the plots of a producer that are in a given lifecycle status, for example the
 * removed (archived) ones that the active inventory no longer lists.
 *
 * @param producerId the owning producer
 * @param status     the lifecycle status the plots must be in
 */
public record GetPlotsByProducerIdAndStatusQuery(ProducerId producerId, PlotStatus status) {

    /**
     * Canonical constructor validating the structural invariants of the query.
     */
    public GetPlotsByProducerIdAndStatusQuery {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (status == null) {
            throw new IllegalArgumentException("plot.status.null");
        }
    }
}
