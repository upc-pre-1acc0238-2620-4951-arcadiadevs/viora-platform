package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.ManagerActivationCodePersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ManagerActivationCodePersistenceEntity}.
 */
@Repository
public interface ManagerActivationCodePersistenceRepository
        extends JpaRepository<ManagerActivationCodePersistenceEntity, UUID> {

    /**
     * Finds a code by its fingerprint and locks its row until the transaction ends.
     *
     * @param codeHash the SHA-256 fingerprint of the code
     * @return the code entity, or empty if no code has this fingerprint
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select code from ManagerActivationCodePersistenceEntity code where code.codeHash = :codeHash")
    Optional<ManagerActivationCodePersistenceEntity> findLockedByCodeHash(@Param("codeHash") String codeHash);

    /**
     * Checks whether a code has this fingerprint.
     *
     * @param codeHash the SHA-256 fingerprint of the code
     * @return true if a code has it
     */
    boolean existsByCodeHash(String codeHash);
}
