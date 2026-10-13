package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.entities.UserAccountPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link UserAccountPersistenceEntity}.
 */
@Repository
public interface UserAccountPersistenceRepository extends JpaRepository<UserAccountPersistenceEntity, UUID> {

    Optional<UserAccountPersistenceEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserAccountPersistenceEntity> findByResetToken(String resetToken);
}
