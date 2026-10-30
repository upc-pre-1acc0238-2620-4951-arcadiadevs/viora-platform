package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;

import java.util.List;

/**
 * The texts of an answer in one language.
 *
 * @param text    the answer text
 * @param facts   the two fact cards
 * @param sources what the answer is based on
 * @param actions links to existing screens
 */
public record RenderedAnswer(String text, List<AnswerFact> facts, List<AnswerSource> sources,
        List<AnswerAction> actions) {
}
