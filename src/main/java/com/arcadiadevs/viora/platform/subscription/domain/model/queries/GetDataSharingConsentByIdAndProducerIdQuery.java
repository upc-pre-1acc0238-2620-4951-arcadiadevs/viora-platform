package com.arcadiadevs.viora.platform.subscription.domain.model.queries;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

/**
 * Domain query record for retrieving a data sharing consent only if it belongs to the producer.
 *
 * @param consentId  the consent identifier value object
 * @param producerId the producer identifier value object
 */
public record GetDataSharingConsentByIdAndProducerIdQuery(DataSharingConsentId consentId, ProducerId producerId) {

    /**
     * Compact constructor validating that neither consentId nor producerId is null.
     *
     * @param consentId  the consent identifier
     * @param producerId the producer identifier
     */
    public GetDataSharingConsentByIdAndProducerIdQuery {
        if (consentId == null) {
            throw new IllegalArgumentException("subscription.consent.id.null_or_empty");
        }
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor from raw UUID strings.
     *
     * @param rawConsentId  the raw UUID string of the consent
     * @param rawProducerId the raw UUID string of the producer
     */
    public GetDataSharingConsentByIdAndProducerIdQuery(String rawConsentId, String rawProducerId) {
        this(new DataSharingConsentId(rawConsentId), new ProducerId(rawProducerId));
    }
}
