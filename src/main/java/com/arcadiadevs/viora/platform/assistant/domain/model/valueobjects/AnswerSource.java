package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * What an answer is based on, shown under it.
 *
 * @param label for example "Based on 5 trees"
 */
public record AnswerSource(String label) {

    /**
     * Compact constructor validating the label.
     */
    public AnswerSource {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("assistant.answer.source.invalid");
        }
    }
}
