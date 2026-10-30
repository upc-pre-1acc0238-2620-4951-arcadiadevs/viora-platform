package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextRejection;

/**
 * The text of a new answer and what wrote it.
 *
 * @param text         the text to show
 * @param model        what wrote it; the template when a model text was rejected
 * @param inputTokens  tokens sent to the model; null when no model was called or it did not say
 * @param outputTokens tokens received from the model; null when no model was called or it did not say
 * @param rejection    why the model text was not used; null when it was used or no model was called
 */
public record WrittenText(String text, AnswerModel model, Integer inputTokens, Integer outputTokens,
        TextRejection rejection) {

    /**
     * The template text, without calling a model.
     *
     * @param templateText the template text
     * @return the written text
     */
    public static WrittenText template(String templateText) {
        return new WrittenText(templateText, AnswerModel.TEMPLATE, null, null, null);
    }
}
