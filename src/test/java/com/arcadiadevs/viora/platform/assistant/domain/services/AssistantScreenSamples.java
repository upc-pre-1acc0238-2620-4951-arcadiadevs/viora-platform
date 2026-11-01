package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlternationFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CampaignClosedFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlanFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SensorsFacts;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * One plot with full figures for each of the eleven screens of the assistant, so that most of its questions are
 * enabled. The live evaluation of the model reads them; the screen tests keep their own, finer samples.
 */
public final class AssistantScreenSamples {

    private AssistantScreenSamples() {
    }

    /**
     * The figures of every screen, in the order of {@code ScreenKey}.
     *
     * @return one set of figures per screen
     */
    public static List<ScreenFacts> everyScreen() {
        return List.of(
                home(),
                alternation(),
                plan(),
                ClimateSamples.alert(ClimateSamples.heat(), ClimateSamples.hotThenMild()),
                ClimateSamples.climate(ClimateSamples.hotThenMild(),
                        List.of(ClimateSamples.virtualProbe(), ClimateSamples.microclimate()),
                        ClimateSamples.reading(16.0)),
                ChillSamples.chill(ChillSamples.inProgress(), ChillSamples.lastWinter()),
                PlotDetailSamples.plot(PlotDetailSamples.thinnedPlan(9100.0), 0, List.of(ClimateSamples.heat()),
                        PlotDetailSamples.hotToday()),
                // A microclimate node transmitting and a soil probe paused from the app
                new SensorsFacts(List.of(SensorsSamples.virtualMicroclimate()), SensorsSamples.reading(),
                        SensorsSamples.coverage(5),
                        List.of(new ClimateData.PausedNode("SOIL_PROBE", 30, LocalDate.of(2026, 10, 20)))),
                SamplingSamples.sampling(SamplingSamples.representative(), 0),
                ThinningDoneSamples.contract().facts(),
                // The closed campaign 2025 and 2024 were settled with their calibers
                new CampaignClosedFacts(CampaignClosedSamples.contractHarvests(), 0.18, "LOW_ALTERNATION",
                        CampaignClosedSamples.outcome("OPTIMAL", 14, 49), 2.0, new TreeMap<>(Map.of(
                                2024, new CampaignClosedFacts.SettledCaliber(205.6, "201/220"),
                                2025, new CampaignClosedFacts.SettledCaliber(180.4, "171/190")))));
    }

    /**
     * The home screen of a producer's whole field: two plots, one in its thinning window with a heat alert and the
     * forecast, the other with its harvests. The daily summary is written from these figures.
     *
     * @return the figures of the whole field
     */
    public static HomeFacts home() {
        return HomeSamples.field(
                HomeSamples.full(HomeSamples.estimated(9100.0), 0, List.of(ClimateSamples.heat()),
                        HomeSamples.harvests(2023, 8700.0, 12450.0, 9600.0), ClimateSamples.hotThenMild()),
                HomeSamples.withYields(HomeSamples.harvests(2023, 4000.0, 10000.0, 5000.0), HomeSamples.INDEX,
                        HomeSamples.MODERATE));
    }

    private static AlternationFacts alternation() {
        var yields = new TreeMap<Integer, Double>();
        yields.put(2023, 40_000.0);
        yields.put(2024, 100_000.0);
        yields.put(2025, 50_000.0);
        return new AlternationFacts(2026, 10.0, yields, 0.50, "MODERATE_ALTERNATION", false);
    }

    private static PlanFacts plan() {
        return new PlanFacts(2026, 0.60, 0.40, 30.0,
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 14), 5, false, null, 0);
    }
}
