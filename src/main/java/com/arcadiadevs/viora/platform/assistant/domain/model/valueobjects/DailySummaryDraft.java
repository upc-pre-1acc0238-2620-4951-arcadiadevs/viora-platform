package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.List;

/**
 * Everything of a daily summary that the server decides, before it is written in a language: the sentences, exactly
 * three fact cards and the source. Every figure is computed from the data of the plot or the field; nothing here
 * comes from a language model.
 *
 * @param text   the sentences of the summary
 * @param facts  exactly three cards
 * @param source what the summary is based on
 */
public record DailySummaryDraft(CatalogText text, List<FactDraft> facts, CatalogText source) {

    /** Number of fact cards of the daily summary: the design draws three cards of the same size. */
    public static final int FACT_COUNT = 3;

    /**
     * Compact constructor validating the shape of the summary.
     */
    public DailySummaryDraft {
        if (text == null || source == null) {
            throw new IllegalArgumentException("assistant.daily_summary.draft.invalid");
        }
        facts = facts == null ? List.of() : List.copyOf(facts);
        if (facts.size() != FACT_COUNT) {
            throw new IllegalArgumentException("assistant.daily_summary.facts.count");
        }
    }
}
