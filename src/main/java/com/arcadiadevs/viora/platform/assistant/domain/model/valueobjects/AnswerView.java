package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;

/**
 * An answer together with whether its data is still current, which depends on the plot today and is never stored.
 *
 * @param answer    the saved answer
 * @param freshness whether the plot still has the data it was written from
 */
public record AnswerView(AssistantAnswer answer, Freshness freshness) {
}
