package com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;

import java.time.Instant;
import java.util.OptionalLong;

/**
 * Port limiting how many codes a producer or a technical manager can try and miss, so a code cannot be guessed by
 * trying them all. Each one has attempts of their own.
 */
public interface RedemptionAttemptLimiter {

    /**
     * Reserves an attempt for the producer, unless the producer already used up the attempts of the window. The
     * check and the reservation happen together, so attempts sent at the same time cannot get past the limit.
     *
     * @param producerId the producer
     * @param at         the instant of the attempt
     * @return empty if the attempt was reserved, or the seconds to wait before trying again
     */
    OptionalLong tryAcquire(ProducerId producerId, Instant at);

    /**
     * Gives back an attempt reserved with {@link #tryAcquire}, when the attempt did not try a code that could not be
     * redeemed (for example the producer was already subscribed or sent a malformed code).
     *
     * @param producerId the producer
     * @param at         the instant the attempt was reserved with
     */
    void release(ProducerId producerId, Instant at);

    /**
     * Forgets the attempts of the producer, after a code is redeemed.
     *
     * @param producerId the producer
     */
    void reset(ProducerId producerId);

    /**
     * Reserves an attempt for a technical manager redeeming a manager code, as {@link #tryAcquire(ProducerId, Instant)}
     * does for a producer.
     *
     * @param managerId the manager
     * @param at        the instant of the attempt
     * @return empty if the attempt was reserved, or the seconds to wait before trying again
     */
    OptionalLong tryAcquire(ManagerId managerId, Instant at);

    /**
     * Gives back an attempt reserved with {@link #tryAcquire(ManagerId, Instant)} that did not try a code that could
     * not be redeemed.
     *
     * @param managerId the manager
     * @param at        the instant the attempt was reserved with
     */
    void release(ManagerId managerId, Instant at);

    /**
     * Forgets the attempts of the manager, after a code is redeemed.
     *
     * @param managerId the manager
     */
    void reset(ManagerId managerId);
}
