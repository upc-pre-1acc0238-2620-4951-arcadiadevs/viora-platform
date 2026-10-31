package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.AlternationQuestions;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collectors;

/**
 * The figures of a plot the alternation screen reads. A figure the plot does not have yet is null (or empty).
 *
 * @param campaignYear     current campaign year
 * @param areaHectares     area of the plot, to turn kilograms into tonnes per hectare
 * @param yieldsKg         kilograms harvested per campaign year, ascending
 * @param bearingIndex     latest Biennial Bearing Index; null while it cannot be calculated yet
 * @param bearingCategory  alternation band of that index
 * @param caliberEstimated whether the caliber projection of the current campaign is estimated; only the catalog
 *                         reads it
 */
public record AlternationFacts(
        int campaignYear,
        Double areaHectares,
        SortedMap<Integer, Double> yieldsKg,
        Double bearingIndex,
        String bearingCategory,
        boolean caliberEstimated
) implements ScreenFacts {

    /**
     * One harvested campaign in tonnes per hectare.
     *
     * @param year        campaign year
     * @param tonnesPerHa tonnes per hectare
     */
    public record Campaign(int year, double tonnesPerHa) {
    }

    /**
     * The campaigns with a harvest, oldest first, in tonnes per hectare.
     *
     * @return the campaigns; empty while the plot has no harvest or no area to divide by
     */
    public List<Campaign> campaigns() {
        if (yieldsKg == null || areaHectares == null || areaHectares <= 0.0) {
            return List.of();
        }
        return yieldsKg.entrySet().stream()
                .map(entry -> new Campaign(entry.getKey(), entry.getValue() / areaHectares / 1000.0))
                .toList();
    }

    /**
     * Counts the campaigns with a registered harvest, whether or not the area is known.
     *
     * @return number of historical campaigns
     */
    public int historicalCampaigns() {
        return yieldsKg == null ? 0 : yieldsKg.size();
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.ALTERNATION;
    }

    @Override
    public String canonical() {
        return "ALTERNATION|" + campaignYear + "|" + areaHectares + "|"
                + (yieldsKg == null ? "" : yieldsKg.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(";")))
                + "|" + bearingIndex + "|" + bearingCategory;
    }

    @Override
    public List<CatalogQuestion> questions() {
        return AlternationQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return AlternationQuestions.answer(questionId, this, plotId);
    }
}
