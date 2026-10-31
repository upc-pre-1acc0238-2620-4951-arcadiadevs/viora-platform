package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Data the HOME questions answer from: one plot, or every active plot of the producer when the question is about
 * the whole field. Every figure is already computed by the context that owns it; this record only groups the plots
 * so the catalog and the assemblers judge them with the same rules.
 *
 * @param fieldScope whether the question is about the producer's whole field instead of one plot
 * @param plots      the plot asked about, or every active plot of the producer
 * @param thresholds the alert thresholds Telemetry uses; null when the forecast was not read
 * @param outcome    outcome of the confirmed thinning of the plot, read only by THINNING_DONE; null otherwise or
 *                   before the thinning is confirmed
 */
public record HomeData(boolean fieldScope, List<PlotHome> plots, ClimateData.Thresholds thresholds,
        ThinningOutcomeData outcome) {

    /** No HOME data: the screens that are not HOME. */
    public static final HomeData NONE = new HomeData(false, List.of(), null);

    /**
     * Data without a thinning outcome.
     */
    public HomeData(boolean fieldScope, List<PlotHome> plots, ClimateData.Thresholds thresholds) {
        this(fieldScope, plots, thresholds, null);
    }

    /**
     * The same data with the outcome of the confirmed thinning.
     *
     * @param thinningOutcome the outcome; null before the thinning is confirmed
     * @return the data
     */
    public HomeData withOutcome(ThinningOutcomeData thinningOutcome) {
        return new HomeData(fieldScope, plots, thresholds, thinningOutcome);
    }

    /** Status of a caliber projection that has figures. */
    public static final String CALIBER_ESTIMATED = "ESTIMATED";

    public HomeData {
        plots = plots == null ? List.of() : List.copyOf(plots);
    }

    /**
     * Active plots the answer covers.
     *
     * @return one for a plot, every active plot of the producer for the field
     */
    public int activePlots() {
        return plots.size();
    }

    /**
     * Whether the texts can speak of one plot: a plot question, or a field with a single active plot.
     *
     * @return true when the answer is about exactly one plot
     */
    public boolean singlePlot() {
        return plots.size() == 1;
    }

    /**
     * The open heat, frost and hydric alerts of the plots; a warm winter belongs to the chill screen, as in ALERT.
     *
     * @return the alerts of every plot
     */
    public List<ClimateData.Incident> openAlerts() {
        return plots.stream().flatMap(plot -> plot.openAlerts().stream())
                .filter(incident -> ClimateData.ALERT_TYPES.contains(incident.type()))
                .toList();
    }

    /**
     * Plots with at least one open heat, frost or hydric alert.
     *
     * @return how many plots
     */
    public int plotsWithAlerts() {
        return (int) plots.stream()
                .filter(plot -> plot.openAlerts().stream()
                        .anyMatch(incident -> ClimateData.ALERT_TYPES.contains(incident.type())))
                .count();
    }

    /**
     * Thinning plans whose caliber projection is estimated, with the caliber the answer shows.
     *
     * @return the plans, in plot order
     */
    public List<ThinningPlanData> caliberEstimates() {
        return plots.stream().map(PlotHome::plan)
                .filter(plan -> plan != null && CALIBER_ESTIMATED.equals(plan.caliberProjectionStatus())
                        && plan.mostLikelyFruitsPerKg() != null && plan.commercialSizeGrade() != null)
                .toList();
    }

    /**
     * The last campaign against the one before it. For the field, the two latest harvest years of any plot, summed
     * only over the plots that registered both, so the same plots are compared.
     *
     * @return the comparison, or empty without two campaigns to compare (or with nothing harvested the first)
     */
    public Optional<CampaignComparison> campaignComparison() {
        var years = new TreeSet<Integer>(Comparator.reverseOrder());
        plots.forEach(plot -> years.addAll(plot.yields().keySet()));
        if (years.size() < 2) {
            return Optional.empty();
        }
        var iterator = years.iterator();
        int last = iterator.next();
        int previous = iterator.next();
        double lastKg = 0.0;
        double previousKg = 0.0;
        int compared = 0;
        for (var plot : plots) {
            var yields = plot.yields();
            if (yields.containsKey(last) && yields.containsKey(previous)) {
                lastKg += yields.get(last);
                previousKg += yields.get(previous);
                compared++;
            }
        }
        if (compared == 0 || previousKg <= 0.0) {
            return Optional.empty();
        }
        return Optional.of(new CampaignComparison(last, lastKg, previous, previousKg, compared));
    }

    /**
     * The last campaign against the one before it.
     *
     * @param lastYear      latest harvest year
     * @param lastKg        kilograms harvested that year by the compared plots
     * @param previousYear  the harvest year before it
     * @param previousKg    kilograms harvested that year by the compared plots
     * @param comparedPlots plots with a harvest registered in both years
     */
    public record CampaignComparison(int lastYear, double lastKg, int previousYear, double previousKg,
            int comparedPlots) {
    }

    /**
     * What HOME reads of one plot.
     *
     * @param plan            thinning plan of the current campaign; null when the plot was not sampled
     * @param treesNeeded     trees still missing for a representative sampling
     * @param openAlerts      open alerts of the plot
     * @param yields          total kilograms per campaign year, ascending
     * @param bearingIndex    latest Biennial Bearing Index; null when it cannot be calculated yet
     * @param bearingCategory alternation band of the index; null without index
     * @param forecast        seven-day forecast; null when unavailable or not read
     */
    public record PlotHome(ThinningPlanData plan, int treesNeeded, List<ClimateData.Incident> openAlerts,
            SortedMap<Integer, Double> yields, Double bearingIndex, String bearingCategory,
            ClimateData.Forecast forecast) {

        public PlotHome {
            openAlerts = openAlerts == null ? List.of() : List.copyOf(openAlerts);
            yields = yields == null
                    ? Collections.emptySortedMap()
                    : Collections.unmodifiableSortedMap(new TreeMap<>(yields));
        }
    }
}
