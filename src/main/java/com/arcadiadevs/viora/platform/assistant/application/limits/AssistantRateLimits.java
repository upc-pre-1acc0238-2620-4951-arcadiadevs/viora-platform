package com.arcadiadevs.viora.platform.assistant.application.limits;

import java.util.OptionalLong;

/**
 * Limits how often each producer may use the assistant, so one account cannot burn the model budget or load the
 * facades of the other contexts. The limits are per producer, not per address: every route needs a session.
 */
public interface AssistantRateLimits {

    /**
     * Counts a question that is not an idempotent replay. A {@code forceRefresh} also counts against its own,
     * smaller limit, in addition to the answers limit.
     *
     * @param producerId   the producer asking
     * @param forceRefresh whether the question skips the cache
     * @return empty when it is allowed, or the seconds to wait before asking again
     */
    OptionalLong tryAnswer(String producerId, boolean forceRefresh);

    /**
     * Counts a read of the question catalog of a screen.
     *
     * @param producerId the producer reading it
     * @return empty when it is allowed, or the seconds to wait before reading it again
     */
    OptionalLong tryCatalog(String producerId);
}
