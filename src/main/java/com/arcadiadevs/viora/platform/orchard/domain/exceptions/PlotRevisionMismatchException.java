package com.arcadiadevs.viora.platform.orchard.domain.exceptions;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Domain exception thrown when an optimistic lock revision mismatch occurs on a plot.
 */
public class PlotRevisionMismatchException extends BusinessRuleException {

    private final PlotId plotId;
    private final long currentRevision;
    private final long expectedRevision;

    /**
     * Constructs a PlotRevisionMismatchException with plot details and revisions.
     *
     * @param plotId           the identifier of the plot
     * @param currentRevision  the actual current revision in the domain
     * @param expectedRevision the revision expected by the client in If-Match
     */
    public PlotRevisionMismatchException(PlotId plotId, long currentRevision, long expectedRevision) {
        super("plot.revision.mismatch");
        this.plotId = plotId;
        this.currentRevision = currentRevision;
        this.expectedRevision = expectedRevision;
    }

    public PlotId getPlotId() {
        return plotId;
    }

    public long getCurrentRevision() {
        return currentRevision;
    }

    public long getExpectedRevision() {
        return expectedRevision;
    }
}
