package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.assemblers.FruitThinningPrescriptionPersistenceAssembler;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.entities.FruitThinningPrescriptionPersistenceEntity;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.repositories.FruitThinningPrescriptionPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter that bridges the domain {@link FruitThinningPrescriptionRepository} port with Spring Data JPA.
 */
@Repository
public class FruitThinningPrescriptionRepositoryImpl implements FruitThinningPrescriptionRepository {

    private final FruitThinningPrescriptionPersistenceRepository prescriptionPersistenceRepository;

    /**
     * Constructs the adapter injecting the persistence repository.
     *
     * @param prescriptionPersistenceRepository the underlying Spring Data JPA persistence repository
     */
    public FruitThinningPrescriptionRepositoryImpl(FruitThinningPrescriptionPersistenceRepository prescriptionPersistenceRepository) {
        this.prescriptionPersistenceRepository = prescriptionPersistenceRepository;
    }

    @Override
    public Optional<FruitThinningPrescription> findById(PrescriptionId id) {
        return prescriptionPersistenceRepository.findById(UUID.fromString(id.prescriptionId()))
                .map(FruitThinningPrescriptionPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<FruitThinningPrescription> findByIdForUpdate(PrescriptionId id) {
        return prescriptionPersistenceRepository.findByIdForUpdate(UUID.fromString(id.prescriptionId()))
                .map(FruitThinningPrescriptionPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<FruitThinningPrescription> findByPlotIdAndCampaignYear(PlotId plotId, CampaignYear year) {
        return prescriptionPersistenceRepository.findByPlotIdAndCampaignYear(UUID.fromString(plotId.plotId()), year.value())
                .map(FruitThinningPrescriptionPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<FruitThinningPrescription> findByPlotIdInAndCampaignYear(List<PlotId> plotIds, CampaignYear year) {
        if (plotIds == null || plotIds.isEmpty() || year == null) {
            return List.of();
        }
        List<UUID> uuids = plotIds.stream()
                .map(id -> UUID.fromString(id.plotId()))
                .toList();
        return prescriptionPersistenceRepository.findByPlotIdInAndCampaignYear(uuids, year.value()).stream()
                .map(FruitThinningPrescriptionPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public FruitThinningPrescription save(FruitThinningPrescription prescription) {
        UUID prescriptionUuid = UUID.fromString(prescription.snapshot().id().prescriptionId());
        Optional<FruitThinningPrescriptionPersistenceEntity> existing = prescriptionPersistenceRepository.findById(prescriptionUuid);

        FruitThinningPrescriptionPersistenceEntity entityToSave;
        if (existing.isPresent()) {
            entityToSave = existing.get();
            FruitThinningPrescriptionPersistenceAssembler.updateEntityFromDomain(entityToSave, prescription);
        } else {
            entityToSave = FruitThinningPrescriptionPersistenceAssembler.toPersistenceFromDomain(prescription);
        }

        FruitThinningPrescriptionPersistenceEntity savedEntity = prescriptionPersistenceRepository.saveAndFlush(entityToSave);
        return FruitThinningPrescriptionPersistenceAssembler.toDomainFromPersistence(savedEntity);
    }
}
