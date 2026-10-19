package com.arcadiadevs.viora.platform.phenology.domain.model.queries;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;

/**
 * Domain query for retrieving the chill accumulation tracker of an orchard plot, with its harvest history and BBI.
 *
 * @param plotId the logical reference to the orchard plot
 */
public record GetChillAccumulationTrackerByPlotIdQuery(PlotId plotId) {
    public GetChillAccumulationTrackerByPlotIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("phenology.plot_id.null");
        }
    }

    /**
     * Creates the query from a raw plot identifier.
     *
     * @param rawPlotId the plot UUID string
     */
    public GetChillAccumulationTrackerByPlotIdQuery(String rawPlotId) {
        this(new PlotId(rawPlotId));
    }
}
