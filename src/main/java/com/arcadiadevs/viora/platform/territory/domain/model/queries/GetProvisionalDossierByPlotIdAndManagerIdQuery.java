package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;

/**
 * Domain query for the provisional dossier of a plot of a member who shares, as data.
 *
 * @param plotId    the requested plot
 * @param managerId the signed-in manager
 */
public record GetProvisionalDossierByPlotIdAndManagerIdQuery(PlotId plotId, ManagerId managerId) {

    /**
     * Compact constructor validating that neither field is null.
     *
     * @param plotId    the requested plot
     * @param managerId the signed-in manager
     */
    public GetProvisionalDossierByPlotIdAndManagerIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("territory.plot.id.null_or_empty");
        }
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor from raw UUID strings.
     *
     * @param rawPlotId    the raw UUID string of the plot
     * @param rawManagerId the raw UUID string of the manager
     */
    public GetProvisionalDossierByPlotIdAndManagerIdQuery(String rawPlotId, String rawManagerId) {
        this(new PlotId(rawPlotId), new ManagerId(rawManagerId));
    }
}
