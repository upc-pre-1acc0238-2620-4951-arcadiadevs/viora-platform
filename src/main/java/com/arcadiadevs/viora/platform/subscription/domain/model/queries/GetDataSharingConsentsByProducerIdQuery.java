package com.arcadiadevs.viora.platform.subscription.domain.model.queries;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

/**
 * Domain query record for retrieving the data sharing consents of a producer.
 *
 * @param producerId the producer identifier value object
 */
public record GetDataSharingConsentsByProducerIdQuery(ProducerId producerId) {

    /**
     * Compact constructor validating that producerId is not null.
     *
     * @param producerId the producer identifier
     */
    public GetDataSharingConsentsByProducerIdQuery {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string.
     *
     * @param rawProducerId the raw UUID string of the producer
     */
    public GetDataSharingConsentsByProducerIdQuery(String rawProducerId) {
        this(new ProducerId(rawProducerId));
    }
}
