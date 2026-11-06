package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.InvitationCodePersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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

    /**
     * Finds a code by its fingerprint without locking it.
     *
     * @param codeHash the SHA-256 fingerprint of the code
     * @return the code entity, or empty if no code has this fingerprint
     */
    @Query("select code from InvitationCodePersistenceEntity code where code.codeHash = :codeHash")
    Optional<InvitationCodePersistenceEntity> findUnlockedByCodeHash(@Param("codeHash") String codeHash);

    /**
     * Finds a code and locks its row until the transaction ends.
     *
     * @param id the code identifier
     * @return the code entity, or empty if it does not exist
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select code from InvitationCodePersistenceEntity code where code.id = :id")
    Optional<InvitationCodePersistenceEntity> findLockedById(@Param("id") UUID id);

    /**
     * Lists the codes of a license, newest first.
     *
     * @param licenseId the license
     * @return the code entities
     */
    @Query("select code from InvitationCodePersistenceEntity code where code.licenseId = :licenseId "
            + "order by coalesce(code.issuedAt, code.createdAt) desc, code.expiresAt desc")
    List<InvitationCodePersistenceEntity> findAllOfLicense(@Param("licenseId") UUID licenseId);

    /**
     * Lists the available codes of a license whose time is over.
     *
     * @param licenseId the license
     * @param at        the instant to check
     * @return the code entities
     */
    @Query("select code from InvitationCodePersistenceEntity code where code.licenseId = :licenseId "
            + "and code.status = com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects"
            + ".InvitationCodeStatus.AVAILABLE and code.expiresAt <= :at")
    List<InvitationCodePersistenceEntity> findOverdue(@Param("licenseId") UUID licenseId, @Param("at") Instant at);

    /**
     * Counts the codes of a license that hold a seat: the redeemed ones and the available ones still in time.
     *
     * @param licenseId the license
     * @param at        the instant to check
     * @return the number of codes
     */
    @Query("select count(code) from InvitationCodePersistenceEntity code where code.licenseId = :licenseId and "
            + "(code.status = com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects"
            + ".InvitationCodeStatus.REDEEMED or (code.status = com.arcadiadevs.viora.platform.subscription.domain"
            + ".model.valueobjects.InvitationCodeStatus.AVAILABLE and code.expiresAt > :at))")
    long countSeatsInUse(@Param("licenseId") UUID licenseId, @Param("at") Instant at);

    /**
     * Checks whether a code has this fingerprint, without locking it.
     *
     * @param codeHash the SHA-256 fingerprint of the code
     * @return true if a code has it
     */
    boolean existsByCodeHash(String codeHash);
}
