package com.arcadiadevs.viora.platform.assistant.domain.model.queries;

import java.util.UUID;

/**
 * Query reopening one answer of the producer from the history.
 *
 * @param producerId producer asking; only its own answers are found
 * @param answerId   the answer
 */
public record GetAnswerByIdQuery(String producerId, UUID answerId) {

    public GetAnswerByIdQuery {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("assistant.producer_id.null");
        }
        if (answerId == null) {
            throw new IllegalArgumentException("assistant.answer.invalid");
        }
    }
}
