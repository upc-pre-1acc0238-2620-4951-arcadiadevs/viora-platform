package com.arcadiadevs.viora.platform.assistant.domain.model.aggregates;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClientRequestId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.time.Instant;
import java.util.List;

/**
 * An answer the assistant gave to a closed question about a plot, kept as it was written so the producer can reopen it.
 * It never changes after it is saved: asking again with new data writes a new answer.
 *
 * @param id               identifier
 * @param producerId       producer who asked
 * @param plotId           plot the question was about
 * @param questionId       question asked
 * @param language         language the text is written in
 * @param dataVersion      fingerprint of the figures it was written from
 * @param text             the 2-3 sentences
 * @param facts            exactly two cards
 * @param sources          what it is based on
 * @param actions          buttons that open screens of the app
 * @param clientRequestId  idempotency key of the ask
 * @param provider         who wrote the text: {@link #TEMPLATE} or a model provider
 * @param modelName        model that wrote it; null for a template
 * @param fallback         whether the text is the template because the model failed or its text was rejected
 * @param inputTokens      tokens read by the model; null for a template
 * @param outputTokens     tokens written by the model; null for a template
 * @param createdAt        when it was written
 */
public record AssistantAnswer(
        AnswerId id,
        String producerId,
        PlotId plotId,
        QuestionId questionId,
        AnswerLanguage language,
        DataVersion dataVersion,
        String text,
        List<AnswerFact> facts,
        List<AnswerSource> sources,
        List<AnswerAction> actions,
        ClientRequestId clientRequestId,
        String provider,
        String modelName,
        boolean fallback,
        Integer inputTokens,
        Integer outputTokens,
        Instant createdAt
) {

    /** Provider of the answers written from the deterministic templates. */
    public static final String TEMPLATE = "TEMPLATE";

    /**
     * Compact constructor validating the answer.
     */
    public AssistantAnswer {
        if (id == null || producerId == null || producerId.isBlank() || plotId == null || questionId == null
                || language == null || dataVersion == null || clientRequestId == null || provider == null
                || provider.isBlank() || createdAt == null) {
            throw new IllegalArgumentException("assistant.answer.invalid");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("assistant.answer.text.invalid");
        }
        facts = facts == null ? List.of() : List.copyOf(facts);
        if (facts.size() != AnswerDraft.FACT_COUNT) {
            throw new IllegalArgumentException("assistant.answer.facts.count");
        }
        sources = sources == null ? List.of() : List.copyOf(sources);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    /**
     * Whether the figures the answer was written from are still the figures of the plot.
     *
     * @param current the version of the data of the plot now
     * @return {@link Freshness#UP_TO_DATE} when it matches, {@link Freshness#STALE} otherwise
     */
    public Freshness freshness(DataVersion current) {
        return dataVersion.equals(current) ? Freshness.UP_TO_DATE : Freshness.STALE;
    }
}
