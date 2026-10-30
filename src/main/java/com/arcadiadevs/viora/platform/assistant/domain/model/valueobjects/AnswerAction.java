package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * An action of a stored answer, already localized.
 *
 * @param type   kind of action, {@code DEEPLINK}
 * @param label  button text
 * @param target route of the app the button opens
 */
public record AnswerAction(String type, String label, String target) {

    public AnswerAction {
        if (type == null || label == null || target == null) {
            throw new IllegalArgumentException("assistant.action.invalid");
        }
    }
}
