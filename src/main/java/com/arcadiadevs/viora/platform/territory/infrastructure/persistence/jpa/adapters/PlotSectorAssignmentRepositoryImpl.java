package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;
import com.arcadiadevs.viora.platform.territory.domain.repositories.PlotSectorAssignmentRepository;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.assemblers.PlotSectorAssignmentPersistenceAssembler;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.PlotSectorAssignmentPersistenceEntity;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.repositories.PlotSectorAssignmentPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter of {@link PlotSectorAssignmentRepository}.
 */
@Repository
public class PlotSectorAssignmentRepositoryImpl implements PlotSectorAssignmentRepository {

    private final PlotSectorAssignmentPersistenceRepository plotSectorAssignmentPersistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param plotSectorAssignmentPersistenceRepository the Spring Data repository
     */
    public PlotSectorAssignmentRepositoryImpl(
            PlotSectorAssignmentPersistenceRepository plotSectorAssignmentPersistenceRepository) {
        this.plotSectorAssignmentPersistenceRepository = plotSectorAssignmentPersistenceRepository;
    }

    @Override
    public Optional<PlotSectorAssignment> findByLicenseIdAndPlotIdForUpdate(LicenseId licenseId, PlotId plotId) {
        return plotSectorAssignmentPersistenceRepository.findLockedByLicenseIdAndPlotId(
                        UUID.fromString(licenseId.licenseId()), UUID.fromString(plotId.plotId()))
                .map(PlotSectorAssignmentPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<PlotSectorAssignment> findAllByLicenseId(LicenseId licenseId) {
        return plotSectorAssignmentPersistenceRepository.findAllByLicenseId(UUID.fromString(licenseId.licenseId()))
                .stream()
                .map(PlotSectorAssignmentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<PlotSectorAssignment> findAllByLicenseIdAndProducerId(LicenseId licenseId, ProducerId producerId) {
        return plotSectorAssignmentPersistenceRepository.findAllByLicenseIdAndProducerId(
                        UUID.fromString(licenseId.licenseId()), UUID.fromString(producerId.producerId()))
                .stream()
                .map(PlotSectorAssignmentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<PlotSectorAssignment> findAllPendingByLicenseIdAndNameForUpdate(LicenseId licenseId,
                                                                               SectorZoneName name) {
        return plotSectorAssignmentPersistenceRepository.findLockedByLicenseIdAndStatusAndNormalizedProposedName(
                        UUID.fromString(licenseId.licenseId()), PlotSectorStatus.PENDING, name.normalized())
                .stream()
                .map(PlotSectorAssignmentPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public PlotSectorAssignment save(PlotSectorAssignment assignment) {
        var id = UUID.fromString(assignment.snapshot().id().plotSectorAssignmentId());
        var entity = plotSectorAssignmentPersistenceRepository.findById(id)
                .orElseGet(PlotSectorAssignmentPersistenceEntity::new);
        var saved = plotSectorAssignmentPersistenceRepository.saveAndFlush(
                PlotSectorAssignmentPersistenceAssembler.toPersistenceFromDomain(entity, assignment));
        return PlotSectorAssignmentPersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public void deleteAllByProducerId(ProducerId producerId) {
        plotSectorAssignmentPersistenceRepository.deleteAllByProducerId(UUID.fromString(producerId.producerId()));
    }
}
