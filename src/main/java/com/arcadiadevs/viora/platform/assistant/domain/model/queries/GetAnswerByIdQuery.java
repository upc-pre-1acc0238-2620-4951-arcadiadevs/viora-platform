package com.arcadiadevs.viora.platform.assistant.domain.model.queries;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerId;

/**
 * Query for one saved answer of a producer.
 *
 * @param producerId producer asking, whose plan must be active
 * @param answerId   the answer
 */
public record GetAnswerByIdQuery(String producerId, AnswerId answerId) {

    /**
     * Compact constructor validating that nothing is missing.
     */
    public GetAnswerByIdQuery {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("assistant.producer_id.null");
        }
        if (answerId == null) {
            throw new IllegalArgumentException("assistant.answer.id.invalid");
        }
    }
}
