package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * What a stored answer is based on, already localized ("Basado en 5 árboles, ronda del 29 de octubre").
 *
 * @param label the source text
 */
public record AnswerSource(String label) {

    public AnswerSource {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("assistant.source.invalid");
        }
    }
}
