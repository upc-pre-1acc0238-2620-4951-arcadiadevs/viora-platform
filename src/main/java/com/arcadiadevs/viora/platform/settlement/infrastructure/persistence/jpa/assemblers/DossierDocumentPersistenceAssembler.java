package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CertificationId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.DossierDocumentPersistenceEntity;

import java.util.UUID;

/** Maps a certified dossier document between the domain and JPA, in both directions. */
public final class DossierDocumentPersistenceAssembler {

    private DossierDocumentPersistenceAssembler() {
    }

    /**
     * Builds the entity of a document.
     *
     * @param certificationId certification the document belongs to
     * @param document        exact PDF bytes
     * @return the new entity
     */
    public static DossierDocumentPersistenceEntity toEntity(CertificationId certificationId,
            DossierDocument document) {
        var entity = new DossierDocumentPersistenceEntity();
        entity.setCertificationId(UUID.fromString(certificationId.certificationId()));
        entity.setContent(document.content());
        return entity;
    }

    /**
     * Rebuilds the document of an entity.
     *
     * @param entity stored document
     * @return the domain document with the exact stored bytes
     */
    public static DossierDocument toDomain(DossierDocumentPersistenceEntity entity) {
        return new DossierDocument(entity.getContent());
    }
}
