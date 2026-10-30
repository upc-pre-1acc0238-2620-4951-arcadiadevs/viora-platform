package com.arcadiadevs.viora.platform.assistant.domain.model.queries;

/**
 * Query for the latest answers of a producer that have not expired.
 *
 * @param producerId producer asking, whose plan must be active
 * @param limit      how many answers at most, between 1 and {@link #MAX_LIMIT}
 */
public record GetAnswerHistoryQuery(String producerId, int limit) {

    /** The history keeps ten answers. */
    public static final int MAX_LIMIT = 10;

    /**
     * Compact constructor validating the limit.
     */
    public GetAnswerHistoryQuery {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("assistant.producer_id.null");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("assistant.limit.invalid");
        }
    }
}
