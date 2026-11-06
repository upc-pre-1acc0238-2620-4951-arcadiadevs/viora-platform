package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.territory.domain.exceptions.SectorZoneNameTakenException;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorZoneRepository;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.assemblers.SectorZonePersistenceAssembler;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.SectorZonePersistenceEntity;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.repositories.SectorZonePersistenceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter of {@link SectorZoneRepository}.
 */
@Repository
public class SectorZoneRepositoryImpl implements SectorZoneRepository {

    private final SectorZonePersistenceRepository sectorZonePersistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param sectorZonePersistenceRepository the Spring Data repository
     */
    public SectorZoneRepositoryImpl(SectorZonePersistenceRepository sectorZonePersistenceRepository) {
        this.sectorZonePersistenceRepository = sectorZonePersistenceRepository;
    }

    @Override
    public Optional<SectorZone> findByIdAndLicenseId(SectorZoneId id, LicenseId licenseId) {
        return sectorZonePersistenceRepository.findByIdAndLicenseId(UUID.fromString(id.sectorZoneId()),
                        UUID.fromString(licenseId.licenseId()))
                .map(SectorZonePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<SectorZone> findByLicenseIdAndName(LicenseId licenseId, SectorZoneName name) {
        return sectorZonePersistenceRepository.findByLicenseIdAndNormalizedName(
                        UUID.fromString(licenseId.licenseId()), name.normalized())
                .map(SectorZonePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<SectorZone> findAllByLicenseId(LicenseId licenseId) {
        return sectorZonePersistenceRepository.findAllByLicenseId(UUID.fromString(licenseId.licenseId())).stream()
                .map(SectorZonePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public SectorZone save(SectorZone zone) {
        var id = UUID.fromString(zone.snapshot().id().sectorZoneId());
        var entity = sectorZonePersistenceRepository.findById(id).orElseGet(SectorZonePersistenceEntity::new);
        try {
            var saved = sectorZonePersistenceRepository.saveAndFlush(
                    SectorZonePersistenceAssembler.toPersistenceFromDomain(entity, zone));
            return SectorZonePersistenceAssembler.toDomainFromPersistence(saved);
        } catch (DataIntegrityViolationException ex) {
            // Another request took the same name between the check and this write.
            throw new SectorZoneNameTakenException();
        }
    }
}
