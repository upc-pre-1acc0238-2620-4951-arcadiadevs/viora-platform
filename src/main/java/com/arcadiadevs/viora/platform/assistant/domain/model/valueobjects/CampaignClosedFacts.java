package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.CampaignClosedQuestions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The figures of a plot the closed campaign screen reads. The closed campaign is the latest one with a recorded
 * harvest, not the calendar year: recording the harvest is what closes it.
 *
 * @param yieldsKg        kilograms harvested per campaign year, ascending
 * @param bearingIndex    latest Biennial Bearing Index; null while it cannot be calculated yet
 * @param bearingCategory alternation band of that index
 * @param outcome         outcome of the confirmed thinning of the closed campaign; null when it was not thinned
 * @param areaHectares    area of the plot polygon; null when unknown
 */
public record CampaignClosedFacts(
        SortedMap<Integer, Double> yieldsKg,
        Double bearingIndex,
        String bearingCategory,
        ThinningOutcomeData outcome,
        Double areaHectares
) implements ScreenFacts {

    public CampaignClosedFacts {
        yieldsKg = yieldsKg == null
                ? Collections.emptySortedMap()
                : Collections.unmodifiableSortedMap(new TreeMap<>(yieldsKg));
    }

    /**
     * The closed campaign: the latest campaign with a recorded harvest.
     *
     * @return its year, or empty without recorded harvests
     */
    public Optional<Integer> closedCampaignYear() {
        return yieldsKg.isEmpty() ? Optional.empty() : Optional.of(yieldsKg.lastKey());
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.CAMPAIGN_CLOSED;
    }

    @Override
    public String canonical() {
        return "CAMPAIGN_CLOSED|" + yieldsKg.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(",")) + "|" + bearingIndex + "|" + bearingCategory + "|"
                + (outcome == null ? null : outcome.canonical()) + "|" + areaHectares;
    }

    @Override
    public List<CatalogQuestion> questions() {
        return CampaignClosedQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return CampaignClosedQuestions.answer(questionId, this);
    }
}
