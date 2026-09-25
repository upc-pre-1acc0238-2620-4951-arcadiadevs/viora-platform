package com.arcadiadevs.viora.platform.orchard.domain.model.queries;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;

/**
 * Domain query record for retrieving all active plots belonging to a producer.
 * Represents an explicit query intention without optional or null parameters.
 *
 * @param producerId the producer identifier value object
 */
public record GetAllActivePlotsByProducerIdQuery(ProducerId producerId) {

    /**
     * Compact constructor validating that producerId is not null.
     *
     * @param producerId the producer identifier
     */
    public GetAllActivePlotsByProducerIdQuery {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string.
     *
     * @param rawProducerId the raw UUID string
     */
    public GetAllActivePlotsByProducerIdQuery(String rawProducerId) {
        this(new ProducerId(rawProducerId));
    }
}
