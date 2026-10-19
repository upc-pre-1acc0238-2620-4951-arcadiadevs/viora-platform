package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.entities.UserAccountPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link UserAccountPersistenceEntity}.
 */
@Repository
public interface UserAccountPersistenceRepository extends JpaRepository<UserAccountPersistenceEntity, UUID> {

    Optional<UserAccountPersistenceEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserAccountPersistenceEntity> findByResetTokenHash(String resetTokenHash);

    List<UserAccountPersistenceEntity> findByStatusAndUpdatedAtBefore(String status, Instant cutoff);
}
