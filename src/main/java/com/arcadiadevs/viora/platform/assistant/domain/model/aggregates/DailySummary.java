package com.arcadiadevs.viora.platform.assistant.domain.model.aggregates;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerSource;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DailySummaryDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DailySummaryId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;

import java.time.Instant;
import java.util.List;

/**
 * The daily summary of the home screen for a producer's plot or whole field, kept as it was written so the rest of the
 * day reads it again while its figures do not change. It is its own aggregate because an {@link AssistantAnswer}
 * carries exactly two cards and the summary three, and because it is never part of the answer history. It never
 * changes after it is saved: new figures write a new summary.
 *
 * @param id           identifier
 * @param producerId   producer it is for
 * @param plotId       plot it is about; null for the producer's whole field
 * @param language     language the text is written in
 * @param dataVersion  fingerprint of the home figures it was written from
 * @param text         the sentences
 * @param facts        exactly three cards
 * @param sources      what it is based on
 * @param provider     who wrote the text: {@link AssistantAnswer#TEMPLATE} or a model provider
 * @param modelName    model that wrote it; null for a template
 * @param fallback     whether the text is the template because the model failed or its text was rejected
 * @param inputTokens  tokens read by the model; null for a template
 * @param outputTokens tokens written by the model; null for a template
 * @param createdAt    when it was written
 */
public record DailySummary(
        DailySummaryId id,
        String producerId,
        PlotId plotId,
        AnswerLanguage language,
        DataVersion dataVersion,
        String text,
        List<AnswerFact> facts,
        List<AnswerSource> sources,
        String provider,
        String modelName,
        boolean fallback,
        Integer inputTokens,
        Integer outputTokens,
        Instant createdAt
) {

    /**
     * Compact constructor validating the summary.
     */
    public DailySummary {
        if (id == null || producerId == null || producerId.isBlank() || language == null || dataVersion == null
                || provider == null || provider.isBlank() || createdAt == null) {
            throw new IllegalArgumentException("assistant.daily_summary.invalid");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("assistant.daily_summary.text.invalid");
        }
        facts = facts == null ? List.of() : List.copyOf(facts);
        if (facts.size() != DailySummaryDraft.FACT_COUNT) {
            throw new IllegalArgumentException("assistant.daily_summary.facts.count");
        }
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
