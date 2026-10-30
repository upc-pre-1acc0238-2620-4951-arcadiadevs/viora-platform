package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

/**
 * The deterministic templates as the writer: every answer shows its template text and no model is called. It is
 * the writer of {@code viora.assistant.provider=template}, the default.
 */
public class TemplateAnswerWriter implements AnswerWriter {

    @Override
    public WrittenText write(AnswerDraft draft) {
        return WrittenText.template(draft.templateText());
    }
}
