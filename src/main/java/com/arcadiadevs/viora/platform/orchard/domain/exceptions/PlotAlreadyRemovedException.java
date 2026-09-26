package com.arcadiadevs.viora.platform.orchard.domain.exceptions;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceConflictException;

/**
 * Domain exception thrown when attempting to remove or deactivate an orchard plot
 * that has already been removed or soft-deleted.
 */
public class PlotAlreadyRemovedException extends ResourceConflictException {

    private final PlotId plotId;

    /**
     * Constructs a PlotAlreadyRemovedException with the conflicting plot identifier.
     *
     * @param plotId the identifier of the already removed plot
     */
    public PlotAlreadyRemovedException(PlotId plotId) {
        super("plot.already_removed");
        this.plotId = plotId;
    }

    public PlotId getPlotId() {
        return plotId;
    }
}
