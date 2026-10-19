package com.arcadiadevs.viora.platform.profiles.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.ProfileId;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.profiles.domain.repositories.ProfileRepository;
import com.arcadiadevs.viora.platform.profiles.infrastructure.persistence.jpa.assemblers.ProfilePersistenceAssembler;
import com.arcadiadevs.viora.platform.profiles.infrastructure.persistence.jpa.repositories.ProfilePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository adapter implementing {@link ProfileRepository} backed by Spring Data JPA.
 */
@Repository
public class ProfileRepositoryImpl implements ProfileRepository {

    private final ProfilePersistenceRepository persistenceRepository;

    public ProfileRepositoryImpl(ProfilePersistenceRepository persistenceRepository) {
        if (persistenceRepository == null) {
            throw new IllegalArgumentException("profile.persistence_repository.null");
        }
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Profile save(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile.aggregate.null");
        }
        var entity = ProfilePersistenceAssembler.toPersistenceFromDomain(profile);
        var savedEntity = persistenceRepository.save(entity);
        return ProfilePersistenceAssembler.toDomainFromPersistence(savedEntity);
    }

    @Override
    public Optional<Profile> findById(ProfileId id) {
        if (id == null) {
            return Optional.empty();
        }
        return persistenceRepository.findById(UUID.fromString(id.profileId()))
                .map(ProfilePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Profile> findByUserId(UserId userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return persistenceRepository.findByUserId(UUID.fromString(userId.userId()))
                .map(ProfilePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByUserId(UserId userId) {
        if (userId == null) {
            return false;
        }
        return persistenceRepository.existsByUserId(UUID.fromString(userId.userId()));
    }

    @Override
    public void delete(Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile.aggregate.null");
        }
        persistenceRepository.deleteById(UUID.fromString(profile.snapshot().id().profileId()));
    }
}
