package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CertificationId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.CertifiedDossierDocumentRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.assemblers.DossierDocumentPersistenceAssembler;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.DossierDocumentPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter of the certified dossier document port. It only persists: it is insert-only and publishes nothing.
 * Storing a second document for the same certification violates the primary key and fails.
 */
@Repository
public class CertifiedDossierDocumentRepositoryImpl implements CertifiedDossierDocumentRepository {

    private final DossierDocumentPersistenceRepository persistenceRepository;

    public CertifiedDossierDocumentRepositoryImpl(DossierDocumentPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public void save(CertificationId certificationId, DossierDocument document) {
        persistenceRepository.saveAndFlush(DossierDocumentPersistenceAssembler.toEntity(certificationId, document));
    }

    @Override
    public Optional<DossierDocument> findByCertificationId(CertificationId certificationId) {
        return persistenceRepository.findById(UUID.fromString(certificationId.certificationId()))
                .map(DossierDocumentPersistenceAssembler::toDomain);
    }
}
