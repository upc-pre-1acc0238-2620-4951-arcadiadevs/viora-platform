package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository port of the cooperative invitation codes.
 */
public interface InvitationCodeRepository {

    /**
     * Finds a code by its fingerprint and locks it until the transaction ends, so two redemptions of the same code
     * run one after the other.
     *
     * @param fingerprint the fingerprint of the code
     * @return the code, or empty if no code has this fingerprint
     */
    Optional<InvitationCode> findByFingerprintForRedemption(CodeFingerprint fingerprint);

    /**
     * Finds a code by its fingerprint without locking it, such as to show what it covers before redeeming it.
     *
     * @param fingerprint the fingerprint of the code
     * @return the code, or empty if no code has this fingerprint
     */
    Optional<InvitationCode> findByFingerprint(CodeFingerprint fingerprint);

    /**
     * Finds a code and locks it until the transaction ends, such as to revoke it.
     *
     * @param id the code identity
     * @return the code, or empty if it does not exist
     */
    Optional<InvitationCode> findByIdForUpdate(InvitationCodeId id);

    /**
     * Lists every code of a license, whatever its status.
     *
     * @param licenseId the license
     * @return the codes, newest batch first
     */
    List<InvitationCode> findAllByLicenseId(CooperativeLicenseId licenseId);

    /**
     * Lists the codes of a license that are still available although their time is over.
     *
     * @param licenseId the license
     * @param at        the instant to check
     * @return the overdue codes
     */
    List<InvitationCode> findOverdueByLicenseId(CooperativeLicenseId licenseId, Instant at);

    /**
     * Counts the seats of a license held by codes: the available ones still in time and the redeemed ones.
     *
     * @param licenseId the license
     * @param at        the instant to check
     * @return the seats in use
     */
    int countSeatsInUse(CooperativeLicenseId licenseId, Instant at);

    /**
     * Checks whether a code with this fingerprint was ever issued, in any license and whatever its status.
     *
     * @param fingerprint the fingerprint of the code
     * @return true if a code has it
     */
    boolean existsByFingerprint(CodeFingerprint fingerprint);

    /**
     * Stores a code.
     *
     * @param code the code to store
     * @return the stored code
     */
    InvitationCode save(InvitationCode code);
}
