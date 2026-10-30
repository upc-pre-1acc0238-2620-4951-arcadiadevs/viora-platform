package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Forecast;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Incident;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData.PlotHome;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.PLOT;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.THRESHOLDS;

/** HOME data of the assembler and template tests: one plot, or a field of several plots. */
public final class HomeDataSamples {

    private HomeDataSamples() {
    }

    /** A plot with nothing read: no plan, alerts, harvests or forecast. */
    public static PlotHome bare() {
        return new PlotHome(null, 0, List.of(), null, null, null, null);
    }

    public static PlotHome withPlan(ThinningPlanData plan, int treesNeeded) {
        return new PlotHome(plan, treesNeeded, List.of(), null, null, null, null);
    }

    public static PlotHome withAlerts(Incident... alerts) {
        return new PlotHome(null, 0, List.of(alerts), null, null, null, null);
    }

    public static PlotHome withForecast(Forecast forecast) {
        return new PlotHome(null, 0, List.of(), null, null, null, forecast);
    }

    public static PlotHome withYields(SortedMap<Integer, Double> yields, Double bearingIndex, String band) {
        return new PlotHome(null, 0, List.of(), yields, bearingIndex, band, null);
    }

    public static PlotHome full(ThinningPlanData plan, int treesNeeded, List<Incident> alerts,
            SortedMap<Integer, Double> yields, Forecast forecast) {
        return new PlotHome(plan, treesNeeded, alerts, yields, 0.5123, "MODERATE_ALTERNATION", forecast);
    }

    /** Sampling started with 3 trees, not yet representative: no prescription. */
    public static ThinningPlanData halfway() {
        return new ThinningPlanData(false, null, 0.57, null, null, null, 3, LocalDate.of(2026, 10, 29), null, null,
                null, null, null);
    }

    /** Thinning confirmed with an estimated caliber; the yield is null when not estimated. */
    public static ThinningPlanData estimated(Double kgPerHa) {
        return new ThinningPlanData(true, 30.0, 0.5714, 0.4, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 10), 5, LocalDate.of(2026, 10, 29), LocalDate.of(2026, 10, 28),
                "ESTIMATED", 180.4, "171/190", kgPerHa);
    }

    public static SortedMap<Integer, Double> harvests(int firstYear, double... kilograms) {
        var yields = new TreeMap<Integer, Double>();
        for (int i = 0; i < kilograms.length; i++) {
            yields.put(firstYear + i, kilograms[i]);
        }
        return Collections.unmodifiableSortedMap(yields);
    }

    /** HOME about one plot. */
    public static PlotAnswerData onePlot(PlotHome plot) {
        return PlotAnswerData.ofHome(PLOT, TODAY, 2026, new HomeData(false, List.of(plot), THRESHOLDS));
    }

    /** HOME about the producer's whole field. */
    public static PlotAnswerData field(PlotHome... plots) {
        return PlotAnswerData.ofHome(null, TODAY, 2026, new HomeData(true, List.of(plots), THRESHOLDS));
    }
}
