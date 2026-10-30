package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * A button under an answer. The assistant only reads: an action opens a screen of the app.
 *
 * @param type   kind of action; only {@code DEEPLINK} exists
 * @param label  button text
 * @param target route of the screen to open, with its query, for example thinning/execute?plotId=...
 */
public record AnswerAction(String type, String label, String target) {

    /** The only kind of action: open a screen of the app. */
    public static final String DEEPLINK = "DEEPLINK";

    /**
     * Compact constructor validating that nothing is missing.
     */
    public AnswerAction {
        if (type == null || type.isBlank() || label == null || label.isBlank() || target == null || target.isBlank()) {
            throw new IllegalArgumentException("assistant.answer.action.invalid");
        }
    }
}
