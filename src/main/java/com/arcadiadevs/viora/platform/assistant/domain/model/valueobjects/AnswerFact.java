package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * One of the two cards under the text of an answer, already written in the language of the answer.
 *
 * @param value the figure shown big, for example 0,57
 * @param label what the figure is
 * @param pill  the short note under the figure
 * @param tone  colour family of the card
 */
public record AnswerFact(String value, String label, String pill, FactTone tone) {

    /**
     * Compact constructor validating that nothing is missing.
     */
    public AnswerFact {
        if (value == null || value.isBlank() || label == null || label.isBlank() || pill == null || pill.isBlank()
                || tone == null) {
            throw new IllegalArgumentException("assistant.answer.fact.invalid");
        }
    }
}
