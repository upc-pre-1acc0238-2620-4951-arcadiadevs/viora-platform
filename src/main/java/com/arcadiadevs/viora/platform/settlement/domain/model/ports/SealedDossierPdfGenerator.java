package com.arcadiadevs.viora.platform.settlement.domain.model.ports;

/**
 * Port rendering the PDF of a dossier sealed by the producer. Implementations live in the infrastructure layer.
 */
public interface SealedDossierPdfGenerator {

    /**
     * Renders the dossier.
     *
     * @param content frozen data of the campaign
     * @return the PDF bytes and their page count
     * @throws com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException when it fails
     */
    RenderedDossier render(SealedDossierContent content);

    /**
     * A rendered PDF.
     *
     * @param content   PDF bytes
     * @param pageCount number of pages
     */
    record RenderedDossier(byte[] content, int pageCount) {
    }
}
