package com.arcadiadevs.viora.platform.subscription.domain.model.queries;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

/**
 * Domain query record for retrieving what a cooperative code would give a producer, without redeeming it. The code is
 * kept as typed, so a malformed one can be refused like any other.
 *
 * @param producerId the producer asking
 * @param code       the code as the producer typed it
 */
public record GetCooperativeCodePreviewQuery(ProducerId producerId, String code) {

    /**
     * Compact constructor validating that neither producerId nor code is null.
     *
     * @param producerId the producer asking
     * @param code       the code as typed
     */
    public GetCooperativeCodePreviewQuery {
        if (producerId == null) {
            throw new IllegalArgumentException("producer.id.null_or_empty");
        }
        if (code == null) {
            throw new IllegalArgumentException("subscription.invitation_code.blank");
        }
    }

    /**
     * Convenience constructor from a raw UUID string.
     *
     * @param rawProducerId the raw UUID string of the producer
     * @param code          the code as typed
     */
    public GetCooperativeCodePreviewQuery(String rawProducerId, String code) {
        this(new ProducerId(rawProducerId), code);
    }
}
