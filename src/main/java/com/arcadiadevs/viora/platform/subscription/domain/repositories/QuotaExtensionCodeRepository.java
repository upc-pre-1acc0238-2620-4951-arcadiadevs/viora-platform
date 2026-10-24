package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtensionCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;

import java.util.Optional;

/**
 * Repository port of the extension codes.
 */
public interface QuotaExtensionCodeRepository {

    /**
     * Finds a code by the fingerprint of what the producer typed.
     *
     * @param fingerprint the fingerprint of the code
     * @return the code, or empty if no code has this fingerprint
     */
    Optional<QuotaExtensionCode> findByFingerprint(CodeFingerprint fingerprint);

    /**
     * Stores a code.
     *
     * @param code the code to store
     * @return the stored code
     */
    QuotaExtensionCode save(QuotaExtensionCode code);
}
