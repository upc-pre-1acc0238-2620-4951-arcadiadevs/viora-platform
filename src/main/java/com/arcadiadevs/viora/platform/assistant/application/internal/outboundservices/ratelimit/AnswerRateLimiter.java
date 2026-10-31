package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ratelimit;

import java.time.Instant;
import java.util.OptionalLong;

/**
 * Port limiting how many new answers a producer can have written, so the free quota of the language model is shared
 * fairly. Only answers that are written count: the same request sent again, or the same data asked again, reads the
 * saved answer and is never limited.
 */
public interface AnswerRateLimiter {

    /**
     * Reserves a new answer for the producer, and also a refresh when the producer asks again ignoring the saved
     * answer, unless one of them is used up inside its window. The check and the reservation happen together, so
     * questions sent at the same time cannot get past the limit.
     *
     * @param producerId   the producer
     * @param forceRefresh whether the ask ignores the saved answer
     * @param at           the instant of the ask
     * @return empty if the answer was reserved, or the seconds to wait before asking again
     */
    OptionalLong tryAcquire(String producerId, boolean forceRefresh, Instant at);
}
