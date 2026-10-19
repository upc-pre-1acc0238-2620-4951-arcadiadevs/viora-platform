package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa.entities.UserSessionPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    /**
     * Deletes in one statement the sessions that expired or were revoked before the cutoff.
     *
     * @param cutoff sessions that ended before this instant are deleted
     * @return how many rows were deleted
     */
    @Modifying
    @Query("delete from UserSessionPersistenceEntity s where s.expiresAt < :cutoff or s.revokedAt < :cutoff")
    int deleteEndedBefore(@Param("cutoff") Instant cutoff);
}
