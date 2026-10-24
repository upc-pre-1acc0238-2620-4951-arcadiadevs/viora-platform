package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionId;

import java.util.Optional;

/**
 * Repository port of the quota extensions.
 */
public interface QuotaExtensionRepository {

    /**
     * Stores an extension.
     *
     * @param extension the extension to store
     * @return the stored extension
     */
    QuotaExtension save(QuotaExtension extension);

    /**
     * Finds an extension.
     *
     * @param id the extension identity
     * @return the extension, or empty if it does not exist
     */
    Optional<QuotaExtension> findById(QuotaExtensionId id);

    /**
     * Checks whether a producer already redeemed an extension code.
     *
     * @param codeId     the extension code
     * @param producerId the producer
     * @return true if the producer has an extension from that code
     */
    boolean existsByExtensionCodeIdAndProducerId(ExtensionCodeId codeId, ProducerId producerId);
}
