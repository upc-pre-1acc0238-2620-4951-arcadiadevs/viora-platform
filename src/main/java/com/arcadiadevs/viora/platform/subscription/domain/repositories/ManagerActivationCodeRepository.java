package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.ManagerActivationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;

import java.util.Optional;

/**
 * Repository port of the manager activation codes.
 */
public interface ManagerActivationCodeRepository {

    /**
     * Finds a code by its fingerprint and locks it until the transaction ends, so two redemptions of the same code
     * run one after the other.
     *
     * @param fingerprint the fingerprint of the code
     * @return the code, or empty if no code has this fingerprint
     */
    Optional<ManagerActivationCode> findByFingerprintForRedemption(CodeFingerprint fingerprint);

    /**
     * Checks whether a code with this fingerprint was ever issued, whatever its status.
     *
     * @param fingerprint the fingerprint of the code
     * @return true if a code has this fingerprint
     */
    boolean existsByFingerprint(CodeFingerprint fingerprint);

    /**
     * Stores a code.
     *
     * @param code the code to store
     * @return the stored code
     */
    ManagerActivationCode save(ManagerActivationCode code);
}
