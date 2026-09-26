package com.arcadiadevs.viora.platform.orchard.domain.model.queries;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;

/**
 * Domain query record for retrieving an orchard plot by its unique identifier and owning producer.
 *
 * @param plotId     the unique plot identifier value object
 * @param producerId the owning producer identifier value object
 */
public record GetPlotByIdQuery(PlotId plotId, ProducerId producerId) {

    /**
     * Compact constructor validating that neither plotId nor producerId is null.
     *
     * @param plotId     the plot identifier
     * @param producerId the producer identifier
     */
    public GetPlotByIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
    }

    /**
     * Convenience factory constructor from raw UUID strings.
     *
     * @param rawPlotId     the raw UUID string of the plot
     * @param rawProducerId the raw UUID string of the producer
     */
    public GetPlotByIdQuery(String rawPlotId, String rawProducerId) {
        this(new PlotId(rawPlotId), new ProducerId(rawProducerId));
    }
}
