package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.DossierDocumentPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/** Spring Data repository for the stored PDF documents of certifications. */
@Repository
public interface DossierDocumentPersistenceRepository extends JpaRepository<DossierDocumentPersistenceEntity, UUID> {
}
