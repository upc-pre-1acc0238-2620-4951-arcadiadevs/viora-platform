package com.arcadiadevs.viora.platform.assistant.domain.model.queries;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;

/**
 * Query listing the newest answers of a producer still kept in the history.
 *
 * @param producerId producer whose answers are listed
 * @param limit      maximum answers, between 1 and {@link AssistantAnswer#HISTORY_SIZE}
 */
public record GetAnswerHistoryQuery(String producerId, int limit) {

    public GetAnswerHistoryQuery {
        if (producerId == null || producerId.isBlank()) {
            throw new IllegalArgumentException("assistant.producer_id.null");
        }
        if (limit < 1 || limit > AssistantAnswer.HISTORY_SIZE) {
            throw new IllegalArgumentException("assistant.limit.invalid");
        }
    }
}
