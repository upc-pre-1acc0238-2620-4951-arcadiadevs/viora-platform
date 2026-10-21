package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.assemblers.CooperativeLicensePersistenceAssembler;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.CooperativeLicensePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter bridging {@link CooperativeLicenseRepository} with Spring Data JPA.
 */
@Repository
public class CooperativeLicenseRepositoryImpl implements CooperativeLicenseRepository {

    private final CooperativeLicensePersistenceRepository persistenceRepository;

    /**
     * Creates the adapter.
     *
     * @param persistenceRepository the Spring Data repository
     */
    public CooperativeLicenseRepositoryImpl(CooperativeLicensePersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<CooperativeLicense> findById(CooperativeLicenseId id) {
        return persistenceRepository.findById(UUID.fromString(id.cooperativeLicenseId()))
                .map(CooperativeLicensePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<CooperativeLicense> findByIdForRedemption(CooperativeLicenseId id) {
        return persistenceRepository.findLockedById(UUID.fromString(id.cooperativeLicenseId()))
                .map(CooperativeLicensePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsAny() {
        return persistenceRepository.count() > 0;
    }

    @Override
    public CooperativeLicense save(CooperativeLicense license) {
        var id = UUID.fromString(license.snapshot().id().cooperativeLicenseId());
        var entity = persistenceRepository.findById(id)
                .map(existing -> CooperativeLicensePersistenceAssembler.updateEntityFromDomain(existing, license))
                .orElseGet(() -> CooperativeLicensePersistenceAssembler.toPersistenceFromDomain(license));
        return CooperativeLicensePersistenceAssembler.toDomainFromPersistence(persistenceRepository.saveAndFlush(entity));
    }
}
