package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.CaliberCalibrationObservationPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for caliber calibration observations. */
@Repository
public interface CaliberCalibrationObservationPersistenceRepository
        extends JpaRepository<CaliberCalibrationObservationPersistenceEntity, UUID> {

    List<CaliberCalibrationObservationPersistenceEntity> findByVariety(String variety);

    Optional<CaliberCalibrationObservationPersistenceEntity> findByPlotIdAndCampaignYear(UUID plotId, Integer campaignYear);
}
