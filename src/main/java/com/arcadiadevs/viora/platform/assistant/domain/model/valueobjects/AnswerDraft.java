package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.List;

/**
 * Everything of an answer that the server decides, before it is written in a language: the sentences, exactly two
 * fact cards, the source and the buttons. The figures are all computed from the data of the plot; nothing here comes
 * from a language model.
 *
 * @param text    the 2-3 sentences of the answer
 * @param facts   exactly two cards
 * @param source  what the answer is based on
 * @param actions buttons that open screens of the app; may be empty
 */
public record AnswerDraft(CatalogText text, List<FactDraft> facts, CatalogText source, List<ActionDraft> actions) {

    /** Number of fact cards under an answer: the design draws two cards of the same size. */
    public static final int FACT_COUNT = 2;

    /**
     * Compact constructor validating the shape of the answer.
     */
    public AnswerDraft {
        if (text == null || source == null) {
            throw new IllegalArgumentException("assistant.answer.draft.invalid");
        }
        facts = facts == null ? List.of() : List.copyOf(facts);
        if (facts.size() != FACT_COUNT) {
            throw new IllegalArgumentException("assistant.answer.facts.count");
        }
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}
