package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;

/**
 * A stored answer with its freshness against the current data of the plot.
 *
 * @param answer    the answer
 * @param freshness whether its data is still current
 */
public record AnswerView(AssistantAnswer answer, Freshness freshness) {

    public AnswerView {
        if (answer == null || freshness == null) {
            throw new IllegalArgumentException("assistant.answer.invalid");
        }
    }
}
