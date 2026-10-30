package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The facts a model writes from, and its text is validated against: the facts of the answer plus interpreted facts
 * that turn the context into explicit rules. Nothing else reaches the model: no producer, plot or action target.
 */
public final class ModelFacts {

    /** Interpreted fact: the closed question answered, for example {@code plan.why_percent}. */
    public static final String QUESTION = "questionId";

    /** Interpreted fact: the language of the answer, {@code es} or {@code en}. */
    public static final String LANGUAGE = "language";

    private ModelFacts() {
    }

    /**
     * Builds the facts of a draft for a model.
     *
     * @param draft the draft
     * @return the facts by name, sorted
     */
    public static SortedMap<String, Object> of(AnswerDraft draft) {
        var facts = new TreeMap<String, Object>(draft.facts());
        facts.put(QUESTION, draft.questionId().value());
        facts.put(LANGUAGE, draft.locale().getLanguage());
        // Only a critical real soil probe reading (climate.irrigate) makes irrigation advisable; every other answer,
        // ALTERNATION and PLAN included, may not mention it (contract, section 0). Even then the producer decides.
        boolean advisable = Boolean.TRUE.equals(draft.facts().get(AnswerTextValidator.IRRIGATION_ADVISABLE));
        facts.put(AnswerTextValidator.IRRIGATION_ADVISABLE, advisable);
        if (advisable) {
            facts.put(AnswerTextValidator.PRODUCER_DECIDES_IRRIGATION, true);
        }
        facts.put(AnswerTextValidator.ALLOWED_ACTIONS, draft.actions().stream().map(AnswerAction::label).toList());
        return Collections.unmodifiableSortedMap(facts);
    }
}
