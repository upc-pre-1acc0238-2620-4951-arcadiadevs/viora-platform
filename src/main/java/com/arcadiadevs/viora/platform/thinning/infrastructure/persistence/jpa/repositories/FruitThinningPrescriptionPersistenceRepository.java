package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.FruitThinningPrescriptionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
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

    /**
     * Finds prescription persistence entities by a collection of plot UUIDs and campaign year.
     *
     * @param plotIds      the plot UUID list
     * @param campaignYear the campaign year
     * @return list of matching prescription entities
     */
    java.util.List<FruitThinningPrescriptionPersistenceEntity> findByPlotIdInAndCampaignYear(java.util.List<UUID> plotIds, Integer campaignYear);

    /** Locks the parent row before reading its execution evidence. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FruitThinningPrescriptionPersistenceEntity p where p.id = :id")
    Optional<FruitThinningPrescriptionPersistenceEntity> findByIdForUpdate(
            @Param("id") UUID id);
}
