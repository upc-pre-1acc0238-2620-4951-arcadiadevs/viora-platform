package com.arcadiadevs.viora.platform.profiles.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.profiles.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link ProfilePersistenceEntity}.
 */
@Repository
public interface ProfilePersistenceRepository extends JpaRepository<ProfilePersistenceEntity, UUID> {

    Optional<ProfilePersistenceEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
