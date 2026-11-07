package com.arcadiadevs.viora.platform.settlement.application.queryservices;

import com.arcadiadevs.viora.platform.settlement.domain.model.ports.ProvisionalDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetProvisionalDossierByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetProvisionalDossierPdfByPlotIdQuery;

import java.util.Optional;

/**
 * Query service port of the provisional dossier: the campaign in progress of a plot, read when asked.
 */
public interface ProvisionalDossierQueryService {

    /**
     * Reads what the provisional dossier of a plot shows now.
     *
     * @param query the plot and the names to print
     * @return the content, or empty if the plot is not active
     */
    Optional<ProvisionalDossierContent> handle(GetProvisionalDossierByPlotIdQuery query);

    /**
     * Reads the provisional dossier of a plot now and renders it as a PDF.
     *
     * @param query the plot and the names to print
     * @return the PDF, or empty if the plot is not active
     */
    Optional<ProvisionalDossierPdf> handle(GetProvisionalDossierPdfByPlotIdQuery query);
}
