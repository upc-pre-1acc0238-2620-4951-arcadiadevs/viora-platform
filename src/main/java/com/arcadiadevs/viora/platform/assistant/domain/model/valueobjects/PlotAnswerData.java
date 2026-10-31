package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Data of a plot the facts assemblers answer from, already computed by the contexts that own it.
 *
 * @param plotId           the plot; null only for a HOME question about the producer's whole field
 * @param today            current day in the producers' time zone
 * @param campaignYear     current campaign year
 * @param bearingIndex     latest Biennial Bearing Index; null when it cannot be calculated yet
 * @param bearingCategory  alternation band of the index ({@code REGULAR}, {@code MODERATE_ALTERNATION},
 *                         {@code SEVERE_ALTERNATION}); null without index
 * @param historicalYields total kilograms per campaign year, ascending
 * @param plan             thinning plan of the current campaign; null when the plot was never sampled
 * @param climate          climate data of the ALERT and CLIMATE screens; {@link ClimateData#NONE} otherwise
 * @param home             data of the HOME screen, one plot or the whole field, and of the plot on PLOT_DETAIL
 *                         and SAMPLING_SUMMARY;
 *                         {@link HomeData#NONE} otherwise
 * @param chill            winter chill data of the WINTER_CHILL screen; {@link ChillData#NONE} otherwise
 */
public record PlotAnswerData(
        PlotId plotId,
        LocalDate today,
        int campaignYear,
        Double bearingIndex,
        String bearingCategory,
        SortedMap<Integer, Double> historicalYields,
        ThinningPlanData plan,
        ClimateData climate,
        HomeData home,
        ChillData chill
) {

    /**
     * Data of a screen that reads no chill data.
     */
    public PlotAnswerData(PlotId plotId, LocalDate today, int campaignYear, Double bearingIndex,
            String bearingCategory, SortedMap<Integer, Double> historicalYields, ThinningPlanData plan,
            ClimateData climate, HomeData home) {
        this(plotId, today, campaignYear, bearingIndex, bearingCategory, historicalYields, plan, climate, home,
                ChillData.NONE);
    }

    /**
     * Data of the WINTER_CHILL screen.
     *
     * @param plotId       the plot
     * @param today        current day in the producers' time zone
     * @param campaignYear current campaign year
     * @param chill        the current and last winter of the plot
     * @return the data
     */
    public static PlotAnswerData ofChill(PlotId plotId, LocalDate today, int campaignYear, ChillData chill) {
        return new PlotAnswerData(plotId, today, campaignYear, null, null, null, null, ClimateData.NONE,
                HomeData.NONE, chill);
    }

    /**
     * Data of a screen that reads no HOME data.
     */
    public PlotAnswerData(PlotId plotId, LocalDate today, int campaignYear, Double bearingIndex,
            String bearingCategory, SortedMap<Integer, Double> historicalYields, ThinningPlanData plan,
            ClimateData climate) {
        this(plotId, today, campaignYear, bearingIndex, bearingCategory, historicalYields, plan, climate,
                HomeData.NONE);
    }

    /**
     * Data of the HOME screen.
     *
     * @param plotId       the plot; null for the whole field
     * @param today        current day in the producers' time zone
     * @param campaignYear current campaign year
     * @param home         the plot or the active plots of the producer
     * @return the data
     */
    public static PlotAnswerData ofHome(PlotId plotId, LocalDate today, int campaignYear, HomeData home) {
        return new PlotAnswerData(plotId, today, campaignYear, null, null, null, null, ClimateData.NONE, home);
    }

    /**
     * Data of a screen about one plot (PLOT_DETAIL, SAMPLING_SUMMARY): the plot as HOME reads it, with its index, harvests and plan also set apart so
     * the questions shared with ALTERNATION read them the same way.
     *
     * @param plotId       the plot
     * @param today        current day in the producers' time zone
     * @param campaignYear current campaign year
     * @param plot         the plot, as HOME reads one plot
     * @return the data
     * @throws IllegalArgumentException with {@code assistant.answer_data.invalid} when the data is not about one plot
     */
    public static PlotAnswerData ofOnePlot(PlotId plotId, LocalDate today, int campaignYear, HomeData plot) {
        if (plotId == null || plot == null || plot.fieldScope() || !plot.singlePlot()) {
            throw new IllegalArgumentException("assistant.answer_data.invalid");
        }
        var home = plot.plots().getFirst();
        return new PlotAnswerData(plotId, today, campaignYear, home.bearingIndex(), home.bearingCategory(),
                home.yields(), home.plan(), ClimateData.NONE, plot);
    }

    /**
     * Data of a screen that reads no climate data.
     */
    public PlotAnswerData(PlotId plotId, LocalDate today, int campaignYear, Double bearingIndex,
            String bearingCategory, SortedMap<Integer, Double> historicalYields, ThinningPlanData plan) {
        this(plotId, today, campaignYear, bearingIndex, bearingCategory, historicalYields, plan, ClimateData.NONE);
    }

    public PlotAnswerData {
        home = home == null ? HomeData.NONE : home;
        // Only a HOME question about the whole field has no plot
        if ((plotId == null && !home.fieldScope()) || today == null) {
            throw new IllegalArgumentException("assistant.answer_data.invalid");
        }
        historicalYields = historicalYields == null
                ? Collections.emptySortedMap()
                : Collections.unmodifiableSortedMap(new TreeMap<>(historicalYields));
        climate = climate == null ? ClimateData.NONE : climate;
        chill = chill == null ? ChillData.NONE : chill;
    }
}
