package com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.entities.HistoricalHarvestEntryPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link HistoricalHarvestEntryPersistenceEntity}.
 */
@Repository
public interface HistoricalHarvestEntryPersistenceRepository extends JpaRepository<HistoricalHarvestEntryPersistenceEntity, UUID> {

    /**
     * Finds all harvest records for a given plot.
     *
     * @param plotId the plot UUID
     * @return list of harvest records ordered by campaign year descending
     */
    List<HistoricalHarvestEntryPersistenceEntity> findAllByPlotIdOrderByCampaignYearDesc(UUID plotId);

    /**
     * Finds a harvest record by plot UUID and campaign year.
     *
     * @param plotId       the plot UUID
     * @param campaignYear the campaign year
     * @return an {@link Optional} containing the entity
     */
    Optional<HistoricalHarvestEntryPersistenceEntity> findByPlotIdAndCampaignYear(UUID plotId, Integer campaignYear);

    /**
     * Checks if a harvest record exists for a specific plot and campaign year.
     *
     * @param plotId       the plot UUID
     * @param campaignYear the campaign year
     * @return true if existing, false otherwise
     */
    boolean existsByPlotIdAndCampaignYear(UUID plotId, Integer campaignYear);
}
