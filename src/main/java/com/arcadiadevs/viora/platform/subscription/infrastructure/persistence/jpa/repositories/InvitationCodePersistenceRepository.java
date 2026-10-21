package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.InvitationCodePersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link InvitationCodePersistenceEntity}.
 */
@Repository
public interface InvitationCodePersistenceRepository extends JpaRepository<InvitationCodePersistenceEntity, UUID> {

    /**
     * Finds a code by its fingerprint and locks its row until the transaction ends.
     *
     * @param codeHash the SHA-256 fingerprint of the code
     * @return the code entity, or empty if no code has this fingerprint
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvitationCodePersistenceEntity> findByCodeHash(String codeHash);
}
