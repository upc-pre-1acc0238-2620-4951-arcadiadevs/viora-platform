package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Data of a plot the facts assemblers answer from, already computed by the contexts that own it.
 *
 * @param plotId           the plot
 * @param today            current day in the producers' time zone
 * @param campaignYear     current campaign year
 * @param bearingIndex     latest Biennial Bearing Index; null when it cannot be calculated yet
 * @param bearingCategory  alternation band of the index ({@code REGULAR}, {@code MODERATE_ALTERNATION},
 *                         {@code SEVERE_ALTERNATION}); null without index
 * @param historicalYields total kilograms per campaign year, ascending
 * @param plan             thinning plan of the current campaign; null when the plot was never sampled
 * @param climate          climate data of the ALERT and CLIMATE screens; {@link ClimateData#NONE} otherwise
 */
public record PlotAnswerData(
        PlotId plotId,
        LocalDate today,
        int campaignYear,
        Double bearingIndex,
        String bearingCategory,
        SortedMap<Integer, Double> historicalYields,
        ThinningPlanData plan,
        ClimateData climate
) {

    /**
     * Data of a screen that reads no climate data.
     */
    public PlotAnswerData(PlotId plotId, LocalDate today, int campaignYear, Double bearingIndex,
            String bearingCategory, SortedMap<Integer, Double> historicalYields, ThinningPlanData plan) {
        this(plotId, today, campaignYear, bearingIndex, bearingCategory, historicalYields, plan, ClimateData.NONE);
    }

    public PlotAnswerData {
        if (plotId == null || today == null) {
            throw new IllegalArgumentException("assistant.answer_data.invalid");
        }
        historicalYields = historicalYields == null
                ? Collections.emptySortedMap()
                : Collections.unmodifiableSortedMap(new TreeMap<>(historicalYields));
        climate = climate == null ? ClimateData.NONE : climate;
    }
}
