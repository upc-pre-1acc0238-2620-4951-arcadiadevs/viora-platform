package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibrationObservation;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.CaliberCalibrationObservationRepository;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.CaliberCalibrationObservationPersistenceEntity;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.repositories.CaliberCalibrationObservationPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Adapter between the calibration observation port and Spring Data JPA. */
@Repository
public class CaliberCalibrationObservationRepositoryImpl implements CaliberCalibrationObservationRepository {

    private final CaliberCalibrationObservationPersistenceRepository persistenceRepository;

    public CaliberCalibrationObservationRepositoryImpl(
            CaliberCalibrationObservationPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public List<CaliberCalibrationObservation> findByVariety(String variety) {
        if (variety == null || variety.isBlank()) {
            return List.of();
        }
        return persistenceRepository.findByVariety(variety.trim().toUpperCase(Locale.ROOT)).stream()
                .map(CaliberCalibrationObservationRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public CaliberCalibrationObservation save(CaliberCalibrationObservation observation) {
        UUID plotId = UUID.fromString(observation.plotId().plotId());
        Integer campaignYear = observation.campaignYear().value();
        var entity = persistenceRepository.findByPlotIdAndCampaignYear(plotId, campaignYear)
                .orElseGet(() -> {
                    var created = new CaliberCalibrationObservationPersistenceEntity();
                    created.setId(UUID.randomUUID());
                    created.setPlotId(plotId);
                    created.setCampaignYear(campaignYear);
                    return created;
                });
        entity.setVariety(observation.variety());
        entity.setResidualFruitsPerShoot(observation.residualFruitsPerShoot());
        entity.setCommercialFruitsPerKg(observation.commercialFruitsPerKg());
        return toDomain(persistenceRepository.save(entity));
    }

    private static CaliberCalibrationObservation toDomain(CaliberCalibrationObservationPersistenceEntity entity) {
        return new CaliberCalibrationObservation(new PlotId(entity.getPlotId().toString()),
                new CampaignYear(entity.getCampaignYear()), entity.getVariety(),
                entity.getResidualFruitsPerShoot(), entity.getCommercialFruitsPerKg());
    }
}
