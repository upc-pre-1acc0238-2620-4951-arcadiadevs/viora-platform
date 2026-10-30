package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.List;
import java.util.Locale;
import java.util.SortedMap;

/**
 * What a writer receives to write the text of a new answer.
 *
 * @param questionId   the question answered
 * @param locale       the language of the answer
 * @param facts        the facts of the answer, as the template uses them
 * @param templateText the template text, shown when no other text can be
 * @param actions      the actions the answer offers, already localized
 */
public record AnswerDraft(QuestionId questionId, Locale locale, SortedMap<String, Object> facts, String templateText,
        List<AnswerAction> actions) {

    public AnswerDraft {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
