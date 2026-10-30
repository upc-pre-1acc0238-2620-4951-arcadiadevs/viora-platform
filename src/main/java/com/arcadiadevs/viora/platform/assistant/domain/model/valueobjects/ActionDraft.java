package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * A button before it is written in a language.
 *
 * @param type   kind of action
 * @param label  button text
 * @param target route of the screen to open
 */
public record ActionDraft(String type, CatalogText label, String target) {

    /**
     * Compact constructor validating that nothing is missing.
     */
    public ActionDraft {
        if (type == null || label == null || target == null || target.isBlank()) {
            throw new IllegalArgumentException("assistant.answer.action.invalid");
        }
    }
}
