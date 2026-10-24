package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.SealedDossierPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SealedDossierPersistenceRepository extends JpaRepository<SealedDossierPersistenceEntity, UUID> {

    Optional<SealedDossierPersistenceEntity> findByPlotIdAndCampaignYear(String plotId, Integer campaignYear);

    List<SealedDossierPersistenceEntity> findByPlotIdOrderByCampaignYearDesc(String plotId);

    Optional<SealedDossierPersistenceEntity> findByVerificationHash(String verificationHash);

    /** Everything of a deleted account, for the account purge. */
    List<SealedDossierPersistenceEntity> findAllByProducerId(String producerId);
}
