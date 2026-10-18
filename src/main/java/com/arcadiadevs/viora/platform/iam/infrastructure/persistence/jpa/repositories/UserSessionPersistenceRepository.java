package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.entities.UserSessionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the {@link UserSessionPersistenceEntity}.
 */
@Repository
public interface UserSessionPersistenceRepository extends JpaRepository<UserSessionPersistenceEntity, UUID> {

    Optional<UserSessionPersistenceEntity> findByRefreshTokenHash(String refreshTokenHash);

    List<UserSessionPersistenceEntity> findByUserIdAndRevokedAtIsNull(UUID userId);
}
