package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * A fact card of an answer not yet localized. The server builds the cards, never the model.
 *
 * @param value big figure of the card
 * @param label what the figure is
 * @param pill  short context shown in a pill
 * @param tone  colour of the card
 */
public record FactCardTemplate(CatalogText value, CatalogText label, CatalogText pill, FactTone tone) {

    public FactCardTemplate {
        if (value == null || label == null || pill == null || tone == null) {
            throw new IllegalArgumentException("assistant.fact.invalid");
        }
    }
}
