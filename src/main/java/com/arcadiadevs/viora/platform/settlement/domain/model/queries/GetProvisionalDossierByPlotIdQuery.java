package com.arcadiadevs.viora.platform.settlement.domain.model.queries;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;

/**
 * Domain query for the provisional dossier of a plot's campaign in progress. Who may ask is decided by the caller.
 *
 * @param plotId           the plot
 * @param holderName       the producer's name to print; null if unknown
 * @param organizationName the organization to print; null if unknown
 */
public record GetProvisionalDossierByPlotIdQuery(PlotId plotId, String holderName, String organizationName) {

    /**
     * Compact constructor validating that plotId is not null.
     *
     * @param plotId           the plot
     * @param holderName       the producer's name
     * @param organizationName the organization
     */
    public GetProvisionalDossierByPlotIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("settlement.plot.id.null");
        }
    }
}
