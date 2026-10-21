package com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

import java.time.Instant;
import java.util.OptionalLong;

/**
 * Port limiting how many codes a producer can try and miss, so a code cannot be guessed by trying them all.
 */
public interface RedemptionAttemptLimiter {

    /**
     * Tells whether the producer must wait before trying another code.
     *
     * @param producerId the producer
     * @param at         the instant of the attempt
     * @return the seconds to wait, or empty if the producer can try now
     */
    OptionalLong retryAfterSeconds(ProducerId producerId, Instant at);

    /**
     * Counts a code the producer tried and could not redeem.
     *
     * @param producerId the producer
     * @param at         the instant of the attempt
     */
    void recordFailure(ProducerId producerId, Instant at);

    /**
     * Forgets the failed attempts of the producer, after a code is redeemed.
     *
     * @param producerId the producer
     */
    void reset(ProducerId producerId);
}
