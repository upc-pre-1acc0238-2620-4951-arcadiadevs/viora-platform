package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

/**
 * Writes the text of a new answer. The template writer returns the template text; a model writer may write its own
 * text from the facts and falls back to the template text whenever that text cannot be shown. Cards, sources and
 * actions never come from a writer: the server builds them.
 */
public interface AnswerWriter {

    /**
     * Writes the text of an answer.
     *
     * @param draft the facts, the language and the template text
     * @return the text and what wrote it
     */
    WrittenText write(AnswerDraft draft);

    /**
     * Whether this writer calls a model. With a model, a stored template answer is a fallback (the model failed or
     * its text was rejected, or it predates the model) and the cache keeps it only for a short time.
     *
     * @return true for a model writer; false for the templates
     */
    default boolean usesModel() {
        return false;
    }
}
