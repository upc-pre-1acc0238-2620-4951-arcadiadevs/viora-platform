package com.arcadiadevs.viora.platform.settlement.domain.repositories;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CertificationId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;

import java.util.Optional;

/**
 * Domain port for the immutable PDF of a certified dossier.
 *
 * <p>Insert-only by design: a stored document is never updated or deleted, so its bytes always match the hash
 * recorded in the certification. It is separate from the report aggregate so that loading a report never loads
 * any document.</p>
 */
public interface CertifiedDossierDocumentRepository {

    /**
     * Stores the document of a certification. A certification has at most one document.
     *
     * @param certificationId certification the document belongs to
     * @param document        exact PDF bytes whose SHA-256 is the certification hash
     */
    void save(CertificationId certificationId, DossierDocument document);

    /**
     * Reads the stored document of a certification.
     *
     * @param certificationId certification to look for
     * @return the stored document, if any
     */
    Optional<DossierDocument> findByCertificationId(CertificationId certificationId);
}
