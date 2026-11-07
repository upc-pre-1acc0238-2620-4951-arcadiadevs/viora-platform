package com.arcadiadevs.viora.platform.settlement.domain.model.ports;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;

/**
 * Port that renders a provisional dossier as a PDF.
 */
public interface ProvisionalDossierPdfGenerator {

    /**
     * Renders the dossier.
     *
     * @param content what it shows
     * @return the PDF bytes
     * @throws DossierRenderingException if the document cannot be built
     */
    byte[] render(ProvisionalDossierContent content);
}
