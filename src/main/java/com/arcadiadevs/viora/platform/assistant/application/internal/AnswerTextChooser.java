package com.arcadiadevs.viora.platform.assistant.application.internal;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ai.AnswerTextWriter;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator.Verdict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decides whose text an answer keeps. The model rephrases the template and its text is kept only when the validator
 * accepts it; a model that fails, or writes a figure the server did not compute, leaves the template, so the producer
 * always gets a correct answer and never an error caused by the model.
 */
@Component
public class AnswerTextChooser {

    private static final Logger LOG = LoggerFactory.getLogger(AnswerTextChooser.class);

    private final AnswerTextWriter writer;

    /**
     * Constructs the chooser.
     *
     * @param writer the language model
     */
    public AnswerTextChooser(AnswerTextWriter writer) {
        this.writer = writer;
    }

    /**
     * The text an answer keeps and who wrote it.
     *
     * @param text         the text
     * @param provider     {@link AssistantAnswer#TEMPLATE} when no model is configured, the provider otherwise
     * @param model        the model; null without one
     * @param fallback     true when a model was asked but its text was not kept
     * @param inputTokens  tokens the model read; null when it was not called or did not say
     * @param outputTokens tokens the model wrote; null when it was not called or did not say
     */
    public record Chosen(String text, String provider, String model, boolean fallback, Integer inputTokens,
                         Integer outputTokens) {
    }

    /**
     * Chooses the text of an answer.
     *
     * @param questionId the question, for the logs
     * @param question   the question as the producer read it
     * @param language   the language of the answer
     * @param template   the text of the template
     * @param facts      the two cards
     * @return the text and who wrote it
     */
    public Chosen choose(QuestionId questionId, String question, AnswerLanguage language, String template,
                         List<AnswerFact> facts) {
        if (!writer.enabled()) {
            return new Chosen(template, AssistantAnswer.TEMPLATE, null, false, null, null);
        }
        var written = writer.rewrite(new AnswerTextWriter.Request(language, question, template, facts));
        if (written.isEmpty()) {
            LOG.info("Assistant answer {} kept the template: the model did not answer", questionId.value());
            return new Chosen(template, writer.provider(), writer.model(), true, null, null);
        }
        var text = AnswerTextValidator.normalize(written.get().text());
        var verdict = AnswerTextValidator.validate(text, template, facts, language);
        if (verdict != Verdict.ACCEPTED) {
            // Never log the text: it carries the figures of a producer's plot
            LOG.info("Assistant answer {} kept the template: the validator rejected the text ({})",
                    questionId.value(), verdict);
            return new Chosen(template, writer.provider(), writer.model(), true, written.get().inputTokens(),
                    written.get().outputTokens());
        }
        return new Chosen(text, writer.provider(), writer.model(), false, written.get().inputTokens(),
                written.get().outputTokens());
    }
}
