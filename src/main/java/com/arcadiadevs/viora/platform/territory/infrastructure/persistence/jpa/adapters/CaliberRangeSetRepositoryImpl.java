package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.CaliberRangeSet;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.repositories.CaliberRangeSetRepository;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.assemblers.CaliberRangeSetPersistenceAssembler;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.entities.CaliberRangeSetPersistenceEntity;
import com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.repositories.CaliberRangeSetPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter of the caliber ranges of each license.
 */
@Repository
public class CaliberRangeSetRepositoryImpl implements CaliberRangeSetRepository {

    private final CaliberRangeSetPersistenceRepository caliberRangeSetPersistenceRepository;

    public CaliberRangeSetRepositoryImpl(CaliberRangeSetPersistenceRepository caliberRangeSetPersistenceRepository) {
        this.caliberRangeSetPersistenceRepository = caliberRangeSetPersistenceRepository;
    }

    @Override
    public Optional<CaliberRangeSet> findByLicenseId(LicenseId licenseId) {
        return caliberRangeSetPersistenceRepository.findByLicenseId(UUID.fromString(licenseId.licenseId()))
                .map(CaliberRangeSetPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public CaliberRangeSet save(CaliberRangeSet set) {
        var id = UUID.fromString(set.snapshot().id().caliberRangeSetId());
        var entity = caliberRangeSetPersistenceRepository.findById(id)
                .orElseGet(CaliberRangeSetPersistenceEntity::new);
        var saved = caliberRangeSetPersistenceRepository.saveAndFlush(
                CaliberRangeSetPersistenceAssembler.toPersistenceFromDomain(entity, set));
        return CaliberRangeSetPersistenceAssembler.toDomainFromPersistence(saved);
    }
}
