package com.arcadiadevs.viora.platform.orchard.domain.model.queries;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;

import java.time.Instant;

/**
 * Domain query record for delta synchronization of plots modified since a specific timestamp.
 * Encapsulates mandatory non-null parameters ensuring strong type safety and explicit intent.
 *
 * @param producerId   the producer identifier value object
 * @param updatedSince the lower-bound timestamp for delta synchronization
 */
public record GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery(ProducerId producerId, Instant updatedSince) {

    /**
     * Compact constructor validating that neither parameter is null.
     *
     * @param producerId   the producer identifier
     * @param updatedSince the timestamp threshold
     */
    public GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (updatedSince == null) {
            throw new IllegalArgumentException("plot.updated_since.null");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string and instant.
     *
     * @param rawProducerId the raw UUID string
     * @param updatedSince  the timestamp threshold
     */
    public GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery(String rawProducerId, Instant updatedSince) {
        this(new ProducerId(rawProducerId), updatedSince);
    }
}
