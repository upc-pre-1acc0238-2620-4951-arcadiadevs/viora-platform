package com.arcadiadevs.viora.platform.orchard.domain.exceptions;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceConflictException;

/**
 * Domain exception thrown when attempting to restore an orchard plot that is not archived.
 */
public class PlotNotRemovedException extends ResourceConflictException {

    private final PlotId plotId;

    /**
     * Constructs a PlotNotRemovedException with the conflicting plot identifier.
     *
     * @param plotId the identifier of the plot that is still active
     */
    public PlotNotRemovedException(PlotId plotId) {
        super("plot.not_removed");
        this.plotId = plotId;
    }

    public PlotId getPlotId() {
        return plotId;
    }
}
