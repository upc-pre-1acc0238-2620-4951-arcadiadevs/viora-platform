package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.FruitThinningPrescriptionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA persistence repository for {@link FruitThinningPrescriptionPersistenceEntity}.
 */
@Repository
public interface FruitThinningPrescriptionPersistenceRepository
        extends JpaRepository<FruitThinningPrescriptionPersistenceEntity, UUID> {

    /**
     * Finds a prescription persistence entity by plot id and campaign year.
     *
     * @param plotId       the plot UUID
     * @param campaignYear the campaign year
     * @return optional containing the entity if found
     */
    Optional<FruitThinningPrescriptionPersistenceEntity> findByPlotIdAndCampaignYear(UUID plotId, Integer campaignYear);
}
