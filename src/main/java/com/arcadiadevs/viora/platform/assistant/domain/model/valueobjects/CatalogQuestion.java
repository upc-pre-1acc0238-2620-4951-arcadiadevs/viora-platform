package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * One question of a screen catalog, already evaluated against the data of the plot.
 *
 * @param questionId     question identifier
 * @param label          question text, with the real figures of the plot
 * @param icon           key of the Viora icon catalog
 * @param enabled        whether there is enough data to answer it
 * @param disabledReason what is missing; null when enabled
 */
public record CatalogQuestion(
        QuestionId questionId,
        CatalogText label,
        String icon,
        boolean enabled,
        CatalogText disabledReason
) {
}
