package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

/**
 * What wrote the text of an answer.
 *
 * @param provider the provider, {@code template} for the deterministic templates
 * @param name     the model or template version
 * @param fallback whether the text came from a template
 */
public record AnswerModel(String provider, String name, boolean fallback) {

    /** The deterministic templates of this version. */
    public static final AnswerModel TEMPLATE = new AnswerModel("template", "template-v1", true);

    public AnswerModel {
        if (provider == null || provider.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("assistant.model.invalid");
        }
    }
}
