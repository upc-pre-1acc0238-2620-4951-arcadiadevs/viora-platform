package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorAlert;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorAlertRepository;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.assemblers.SectorAlertPersistenceAssembler;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.SectorAlertPersistenceEntity;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.repositories.SectorAlertPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA adapter of {@link SectorAlertRepository}.
 */
@Repository
public class SectorAlertRepositoryImpl implements SectorAlertRepository {

    private final SectorAlertPersistenceRepository sectorAlertPersistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param sectorAlertPersistenceRepository the Spring Data repository
     */
    public SectorAlertRepositoryImpl(SectorAlertPersistenceRepository sectorAlertPersistenceRepository) {
        this.sectorAlertPersistenceRepository = sectorAlertPersistenceRepository;
    }

    @Override
    public List<SectorAlert> findOpenByLicenseId(LicenseId licenseId) {
        return sectorAlertPersistenceRepository.findAllByLicenseIdAndNormalizedAtIsNull(
                        UUID.fromString(licenseId.licenseId())).stream()
                .map(SectorAlertPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public List<SectorAlert> findOpenOrNormalizedSince(LicenseId licenseId, Instant since) {
        return sectorAlertPersistenceRepository.findOpenOrNormalizedSince(UUID.fromString(licenseId.licenseId()),
                        since).stream()
                .map(SectorAlertPersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public SectorAlert save(SectorAlert alert) {
        var id = UUID.fromString(alert.snapshot().id().sectorAlertId());
        var entity = sectorAlertPersistenceRepository.findById(id).orElseGet(SectorAlertPersistenceEntity::new);
        var saved = sectorAlertPersistenceRepository.saveAndFlush(
                SectorAlertPersistenceAssembler.toPersistenceFromDomain(entity, alert));
        return SectorAlertPersistenceAssembler.toDomainFromPersistence(saved);
    }
}
