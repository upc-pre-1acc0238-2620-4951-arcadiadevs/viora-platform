package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ai;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port to the language model that rephrases the template of an answer. The application never depends on a
 * provider: a model, another provider or none at all are interchangeable behind it.
 */
public interface AnswerTextWriter {

    /**
     * What the model gets: the question, the cards and the template it rephrases. Nothing that identifies the
     * producer or the plot is sent.
     *
     * @param language the language of the answer
     * @param question the question as the producer read it
     * @param template the text of the template
     * @param facts    the two cards
     */
    record Request(AnswerLanguage language, String question, String template, List<AnswerFact> facts) {
    }

    /**
     * What the model wrote.
     *
     * @param text         the text, not yet validated
     * @param inputTokens  tokens it read; null when the provider does not say
     * @param outputTokens tokens it wrote; null when the provider does not say
     */
    record Written(String text, Integer inputTokens, Integer outputTokens) {
    }

    /**
     * Whether a model is configured. Without one, every answer is the template.
     *
     * @return true when the model can be called
     */
    boolean enabled();

    /**
     * Name of the provider, for the answer and the logs.
     *
     * @return for example {@code GROQ}
     */
    String provider();

    /**
     * Name of the model.
     *
     * @return for example {@code openai/gpt-oss-120b}
     */
    String model();

    /**
     * Asks the model to rephrase the template.
     *
     * @param request what the model gets
     * @return what it wrote, or empty when it failed, timed out or refused for any reason
     */
    Optional<Written> rewrite(Request request);
}
