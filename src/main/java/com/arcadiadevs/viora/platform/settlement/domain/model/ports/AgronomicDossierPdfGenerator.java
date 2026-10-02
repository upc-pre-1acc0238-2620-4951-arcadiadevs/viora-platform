package com.arcadiadevs.viora.platform.settlement.domain.model.ports;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;

/**
 * Output port rendering the certified dossier of a campaign as PDF bytes.
 *
 * <p>Deviation from the original audit signature {@code renderPdf(AgronomicReport, AuditorSignature)}: the port
 * receives an {@link AgronomicDossierContent}, a record of frozen data, instead of the live aggregate. This keeps
 * the PDF tied to the exact cut being certified (a later settlement cannot leak into it) and lets the renderer
 * depend on plain data only. Implementations must render the same visible content for the same input: fixed
 * section order and dates taken from the content, never from the system clock. Byte equality between two renders
 * is not guaranteed (a library may generate a file identifier), so the certified artifact is the stored bytes,
 * never a later re-render.</p>
 */
public interface AgronomicDossierPdfGenerator {

    /**
     * Renders the dossier.
     *
     * @param content frozen data of the certified campaign
     * @return the bytes of a complete PDF document
     * @throws DossierRenderingException if the document cannot be produced
     */
    byte[] renderPdf(AgronomicDossierContent content);
}
