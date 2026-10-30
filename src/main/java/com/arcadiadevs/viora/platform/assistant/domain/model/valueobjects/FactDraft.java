package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * A fact card before it is written in a language: three texts and the colour.
 *
 * @param value the figure
 * @param label what the figure is
 * @param pill  the note under it
 * @param tone  colour family
 */
public record FactDraft(CatalogText value, CatalogText label, CatalogText pill, FactTone tone) {

    /**
     * Compact constructor validating that nothing is missing.
     */
    public FactDraft {
        if (value == null || label == null || pill == null || tone == null) {
            throw new IllegalArgumentException("assistant.answer.fact.invalid");
        }
    }
}
