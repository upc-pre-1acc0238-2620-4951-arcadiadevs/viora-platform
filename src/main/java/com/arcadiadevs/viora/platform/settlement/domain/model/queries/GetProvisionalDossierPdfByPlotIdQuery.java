package com.arcadiadevs.viora.platform.settlement.domain.model.queries;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;

/**
 * Domain query for the provisional dossier of a plot as a PDF. Who may ask is decided by the caller.
 *
 * @param plotId           the plot
 * @param holderName       the producer's name to print; null if unknown
 * @param organizationName the organization to print; null if unknown
 */
public record GetProvisionalDossierPdfByPlotIdQuery(PlotId plotId, String holderName, String organizationName) {

    /**
     * Compact constructor validating that plotId is not null.
     *
     * @param plotId           the plot
     * @param holderName       the producer's name
     * @param organizationName the organization
     */
    public GetProvisionalDossierPdfByPlotIdQuery {
        if (plotId == null) {
            throw new IllegalArgumentException("settlement.plot.id.null");
        }
    }
}
