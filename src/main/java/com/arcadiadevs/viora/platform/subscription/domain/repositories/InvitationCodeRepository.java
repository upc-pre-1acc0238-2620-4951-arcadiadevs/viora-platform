package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;

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
     * Stores a code.
     *
     * @param code the code to store
     * @return the stored code
     */
    InvitationCode save(InvitationCode code);
}
