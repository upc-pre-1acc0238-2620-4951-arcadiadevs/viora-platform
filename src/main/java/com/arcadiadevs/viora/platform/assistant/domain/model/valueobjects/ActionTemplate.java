package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * An action of an answer not yet localized. The assistant only reads, so its actions are links to existing screens.
 *
 * @param type   kind of action; only {@code DEEPLINK} for now
 * @param label  button text
 * @param target route of the app the button opens
 */
public record ActionTemplate(String type, CatalogText label, String target) {

    /** Opens an existing screen of the app. */
    public static final String DEEPLINK = "DEEPLINK";

    public ActionTemplate {
        if (type == null || type.isBlank() || label == null || target == null || target.isBlank()) {
            throw new IllegalArgumentException("assistant.action.invalid");
        }
    }
}
