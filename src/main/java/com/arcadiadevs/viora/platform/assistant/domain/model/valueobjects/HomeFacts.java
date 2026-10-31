package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.HomeQuestions;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The figures the home screen reads: of one plot, or of every active plot of the producer when the question is about
 * the whole field. Every figure is already computed by the context that owns it; this record only groups the plots so
 * the catalog and the answers judge them with the same rules.
 *
 * @param today        the current day in the producers' time zone, which the thinning windows and the week are
 *                     judged against
 * @param campaignYear current campaign year
 * @param fieldScope   whether the figures are of the producer's whole field instead of one plot
 * @param plots        the plot asked about, or every active plot of the producer
 * @param thresholds   the alert thresholds Telemetry publishes
 */
public record HomeFacts(
        LocalDate today,
        int campaignYear,
        boolean fieldScope,
        List<PlotHome> plots,
        ClimateData.Thresholds thresholds
) implements ScreenFacts {

    public HomeFacts {
        if (today == null) {
            throw new IllegalArgumentException("assistant.home.invalid");
        }
        plots = plots == null ? List.of() : List.copyOf(plots);
    }

    /**
     * Active plots the answers cover.
     *
     * @return one for a plot, every active plot of the producer for the field
     */
    public int activePlots() {
        return plots.size();
    }

    /**
     * Whether the texts can speak of one plot: a plot question, or a field with a single active plot.
     *
     * @return true when the answers are about exactly one plot
     */
    public boolean singlePlot() {
        return plots.size() == 1;
    }

    /**
     * The open heat, frost and hydric alerts of the plots.
     *
     * @return the alerts of every plot, in plot order
     */
    public List<ClimateData.Incident> openAlerts() {
        return plots.stream().flatMap(plot -> plot.openAlerts().stream()).toList();
    }

    /**
     * Plots with at least one open heat, frost or hydric alert.
     *
     * @return how many plots
     */
    public int plotsWithAlerts() {
        return (int) plots.stream().filter(plot -> !plot.openAlerts().isEmpty()).count();
    }

    /**
     * Thinning plans whose caliber after thinning is estimated, with the figures the answer shows.
     *
     * @return the plans, in plot order
     */
    public List<ThinningPlanData> caliberEstimates() {
        return plots.stream().map(PlotHome::plan)
                .filter(plan -> plan != null && plan.caliberEstimated())
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

    @Override
    public ScreenKey screen() {
        return ScreenKey.HOME;
    }

    @Override
    public String canonical() {
        return "HOME|" + today + "|" + campaignYear + "|" + (fieldScope ? "FIELD" : "PLOT") + "|"
                + (thresholds == null ? null : thresholds.canonical()) + "|"
                + plots.stream().map(PlotHome::canonical).collect(Collectors.joining("/"));
    }

    @Override
    public List<CatalogQuestion> questions() {
        return HomeQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return HomeQuestions.answer(questionId, this);
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
     * What the home screen reads of one plot. A figure the plot does not have yet is null (or empty).
     *
     * @param plan            thinning plan of the campaign; null when the plot was not sampled
     * @param treesNeeded     trees still missing for a representative sampling
     * @param openAlerts      open heat, frost and hydric alerts of the plot
     * @param yields          total kilograms per campaign year, ascending
     * @param bearingIndex    latest Biennial Bearing Index; null while it cannot be calculated yet
     * @param bearingCategory alternation band of that index
     * @param forecast        the seven-day forecast; null when unavailable
     */
    public record PlotHome(
            ThinningPlanData plan,
            int treesNeeded,
            List<ClimateData.Incident> openAlerts,
            SortedMap<Integer, Double> yields,
            Double bearingIndex,
            String bearingCategory,
            ClimateData.Forecast forecast
    ) {

        public PlotHome {
            openAlerts = openAlerts == null ? List.of() : openAlerts.stream()
                    .filter(incident -> ClimateData.ALERT_TYPES.contains(incident.type()))
                    .toList();
            yields = yields == null
                    ? Collections.emptySortedMap()
                    : Collections.unmodifiableSortedMap(new TreeMap<>(yields));
        }

        String canonical() {
            return (plan == null ? null : plan.canonical()) + ";" + treesNeeded + ";"
                    + openAlerts.stream().map(ClimateData.Incident::canonical).collect(Collectors.joining(","))
                    + ";" + yields.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining(","))
                    + ";" + bearingIndex + ";" + bearingCategory + ";"
                    + (forecast == null ? null : forecast.canonical());
        }
    }
}
