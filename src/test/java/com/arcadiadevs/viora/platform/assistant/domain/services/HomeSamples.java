package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Forecast;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Incident;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts.PlotHome;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.time.LocalDate;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.TODAY;

/**
 * Figures of the home screen on 30 October 2026, campaign 2026: one plot, or a field of several plots.
 */
final class HomeSamples {

    static final int CAMPAIGN = 2026;
    static final double INDEX = 0.5123;
    static final String MODERATE = "MODERATE_ALTERNATION";

    private HomeSamples() {
    }

    /** A plot with nothing read: no plan, alerts, harvests or forecast. */
    static PlotHome bare() {
        return new PlotHome(null, 0, List.of(), null, null, null, null);
    }

    static PlotHome withPlan(ThinningPlanData plan, int treesNeeded) {
        return new PlotHome(plan, treesNeeded, List.of(), null, null, null, null);
    }

    static PlotHome withAlerts(Incident... alerts) {
        return new PlotHome(null, 0, List.of(alerts), null, null, null, null);
    }

    static PlotHome withForecast(Forecast forecast) {
        return new PlotHome(null, 0, List.of(), null, null, null, forecast);
    }

    static PlotHome withYields(SortedMap<Integer, Double> yields, Double bearingIndex, String category) {
        return new PlotHome(null, 0, List.of(), yields, bearingIndex, category, null);
    }

    /** Every figure, with the moderate index 0.5123. */
    static PlotHome full(ThinningPlanData plan, int treesNeeded, List<Incident> alerts,
                         SortedMap<Integer, Double> yields, Forecast forecast) {
        return new PlotHome(plan, treesNeeded, alerts, yields, INDEX, MODERATE, forecast);
    }

    /** Prescription issued: remove 30 %, window 20 Oct - 10 Nov (open today), 5 trees. */
    static ThinningPlanData openPlan() {
        return plan(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10));
    }

    static ThinningPlanData plan(LocalDate opensOn, LocalDate closesOn) {
        return new ThinningPlanData(true, 30.0, 0.5714, 0.4, opensOn, closesOn, 5, LocalDate.of(2026, 10, 29), null,
                null, null, null, null);
    }

    /** Sampling started with 3 trees, not yet representative: no prescription. */
    static ThinningPlanData halfway() {
        return new ThinningPlanData(false, null, 0.57, null, null, null, 3, LocalDate.of(2026, 10, 29), null, null,
                null, null, null);
    }

    /** Thinning recorded on 28 Oct with an estimated caliber; the yield is null when not estimated. */
    static ThinningPlanData estimated(Double kgPerHa) {
        return new ThinningPlanData(true, 30.0, 0.5714, 0.4, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10),
                5, LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 28), "ESTIMATED", 180.4, "171/190", kgPerHa);
    }

    static SortedMap<Integer, Double> harvests(int firstYear, double... kilograms) {
        var yields = new TreeMap<Integer, Double>();
        for (int i = 0; i < kilograms.length; i++) {
            yields.put(firstYear + i, kilograms[i]);
        }
        return yields;
    }

    /** Home about one plot. */
    static HomeFacts onePlot(PlotHome plot) {
        return new HomeFacts(TODAY, CAMPAIGN, false, List.of(plot), THRESHOLDS);
    }

    /** Home about the producer's whole field. */
    static HomeFacts field(PlotHome... plots) {
        return new HomeFacts(TODAY, CAMPAIGN, true, List.of(plots), THRESHOLDS);
    }
}
