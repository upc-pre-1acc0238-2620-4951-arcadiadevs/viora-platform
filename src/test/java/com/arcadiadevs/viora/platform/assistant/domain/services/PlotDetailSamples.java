package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Forecast;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.ForecastDay;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Incident;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotDetailFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.time.LocalDate;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.TODAY;

/**
 * Figures of one plot on 30 October 2026, campaign 2026: the contract example "vecería 0,51 + 12 días" is an index of
 * 0.5123 (moderate) with a window open until 10 November.
 */
final class PlotDetailSamples {

    static final int CAMPAIGN = 2026;
    static final double INDEX = 0.5123;
    static final String MODERATE = "MODERATE_ALTERNATION";

    private PlotDetailSamples() {
    }

    /** Prescription issued: remove 30 % (0.5714 measured against 0.40), window 20 Oct - 10 Nov, 5 trees. */
    static ThinningPlanData openPlan() {
        return issuedPlan(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10));
    }

    static ThinningPlanData issuedPlan(LocalDate opensOn, LocalDate closesOn) {
        return new ThinningPlanData(true, 30.0, 0.5714, 0.4, opensOn, closesOn, 5, LocalDate.of(2026, 10, 29), null,
                null, null, null, null);
    }

    /** Sampled, prescription not issued yet. */
    static ThinningPlanData samplingPlan(int trees, LocalDate lastSampledOn) {
        return new ThinningPlanData(false, null, null, null, null, null, trees, lastSampledOn, null, null, null, null,
                null);
    }

    /** Thinning confirmed on 28 Oct with a caliber of 180.4 fruits per kilo (171/190) and the given yield. */
    static ThinningPlanData thinnedPlan(Double kgPerHa) {
        return new ThinningPlanData(true, 30.0, 0.5714, 0.4, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10), 5,
                LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 28), "ESTIMATED", 180.4, "171/190", kgPerHa);
    }

    /** Today: 34.2 °C maximum and 14 °C minimum, over the 32 °C heat threshold. */
    static Forecast hotToday() {
        return todayForecast(34.2, 14.0, true, false);
    }

    static Forecast todayForecast(double max, double min, boolean heatRisk, boolean frostRisk) {
        return new Forecast(TODAY, List.of(new ForecastDay(TODAY, max, min, heatRisk, frostRisk),
                new ForecastDay(TODAY.plusDays(1), 28.0, 12.0, false, false)));
    }

    /** A plot with an index, two harvests and the given plan, alerts and forecast. */
    static PlotDetailFacts plot(ThinningPlanData plan, int treesNeeded, List<Incident> alerts, Forecast forecast) {
        return new PlotDetailFacts(TODAY, CAMPAIGN, plan, treesNeeded, INDEX, MODERATE, 2, alerts, THRESHOLDS,
                forecast);
    }

    /** A plot with no bearing index and no harvests. */
    static PlotDetailFacts withoutIndex(ThinningPlanData plan, int treesNeeded, List<Incident> alerts,
                                        Forecast forecast) {
        return new PlotDetailFacts(TODAY, CAMPAIGN, plan, treesNeeded, null, null, 0, alerts, THRESHOLDS, forecast);
    }
}
