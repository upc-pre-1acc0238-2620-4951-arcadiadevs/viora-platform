package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;

/**
 * Outcome of certifying a campaign: the certification metadata appended to the report and the exact PDF bytes
 * that were hashed.
 *
 * <p>It lives next to {@link DossierCertificationSnapshot} because it references it. The aggregate keeps only the
 * metadata; the caller stores the document through
 * {@link com.arcadiadevs.viora.platform.settlement.domain.repositories.CertifiedDossierDocumentRepository}.</p>
 *
 * @param certification metadata of the new certification, whose hash is the SHA-256 of {@code document}
 * @param document      exact rendered PDF bytes to store immutably
 */
public record CertifiedDossier(DossierCertificationSnapshot certification, DossierDocument document) {

    public CertifiedDossier {
        if (certification == null || document == null) {
            throw new IllegalArgumentException("settlement.certification.reference.null");
        }
    }
}
