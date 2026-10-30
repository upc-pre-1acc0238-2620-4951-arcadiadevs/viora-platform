package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Incident;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.plan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.forecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.frost;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.hotThenMild;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.hydric;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("HOME facts assemblers: one plot or the whole field")
class HomeFactsAssemblersTest {

    private static final String PREFIX = "assistant.template.home.";
    private final HomeWeekPlanFactsAssembler weekPlan = new HomeWeekPlanFactsAssembler();
    private final HomeAlertsAffectingFactsAssembler alerts = new HomeAlertsAffectingFactsAssembler();
    private final HomeCampaignVsPreviousFactsAssembler campaign = new HomeCampaignVsPreviousFactsAssembler();
    private final HomeHarvestEstimateFactsAssembler harvest = new HomeHarvestEstimateFactsAssembler();

    @Test
    @DisplayName("The four HOME questions are registered, one assembler each")
    void registered() {
        var registry = FactsAssemblerRegistry.standard();

        assertThat(List.of(QuestionId.HOME_WEEK_PLAN, QuestionId.HOME_CAMPAIGN_VS_PREVIOUS,
                QuestionId.HOME_ALERTS_AFFECTING, QuestionId.HOME_HARVEST_ESTIMATE))
                .allSatisfy(id -> assertThat(registry.find(id)).map(FactsAssembler::questionId).contains(id));
    }

    @Test
    @DisplayName("A data of another screen never answers a HOME question")
    void onlyHomeData() {
        PlotAnswerData other = AnswerDataSamples.plan(issuedPlan());

        assertThat(List.<FactsAssembler>of(weekPlan, alerts, campaign, harvest))
                .allSatisfy(assembler -> assertThat(assembler.assemble(other)).isEmpty());
    }

    @Test
    @DisplayName("week_plan of a plot: open window with the percent, hottest day against the threshold, its alert")
    void weekPlanOfAPlot() {
        var facts = weekPlan.assemble(onePlot(full(issuedPlan(), 0, List.of(heat()), null, hotThenMild())))
                .orElseThrow();

        assertThat(facts.facts()).containsExactly(
                entry("heatReached", true), entry("heatThreshold", 32.0), entry("hottestMax", 35.1),
                entry("hottestOn", "2026-10-31"), entry("incidentType", "HEAT_WAVE"), entry("openAlerts", 1),
                entry("percentToRemove", 30L), entry("plotsSamplingHalfway", 0), entry("plotsWindowOpen", 1),
                entry("plotsWindowOpening", 0), entry("plotsWithAlerts", 1), entry("scope", "PLOT"),
                entry("severity", "WARNING"), entry("windowClosesOn", "2026-11-10"));
        assertThat(facts.text().key()).isEqualTo(PREFIX + "week_plan.text");
        assertThat(sentence(facts, 0)).isEqualTo(CatalogText.of(PREFIX + "week_plan.thinning.open",
                TextDate.of(LocalDate.of(2026, 11, 10)), 30L));
        assertThat(sentence(facts, 1).key()).isEqualTo(PREFIX + "week_plan.forecast.heat");
        assertThat(sentence(facts, 2).key()).isEqualTo(PREFIX + "week_plan.alerts.some");
        assertThat(facts.cards()).extracting(card -> card.value().key()).containsExactly(
                "assistant.template.value.measure", "assistant.template.value.integer");
        assertThat(facts.cards().get(1).pill().key()).isEqualTo("assistant.template.card.this_plot.pill");
        assertThat(facts.sources()).extracting(CatalogText::key).containsExactly(
                "assistant.template.source.forecast", "assistant.template.source.plans",
                "assistant.template.source.open_alerts");
    }

    @Test
    @DisplayName("week_plan of the field: plots counted, the earliest closing window, the hottest day of any plot")
    void weekPlanOfTheField() {
        var closesFirst = plan(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 5), null);
        var facts = weekPlan.assemble(field(withPlan(issuedPlan(), 0), withPlan(closesFirst, 0),
                withPlan(halfway(), 2), full(null, 0, List.of(heat(), frost()), null,
                        forecast(30.0, 31.0, 36.5, 30.0, 29.0, 28.0, 27.0)),
                withAlerts(hydric()))).orElseThrow();

        assertThat(facts.facts()).contains(entry("scope", "FIELD"), entry("activePlots", 5),
                entry("plotsWindowOpen", 2), entry("plotsSamplingHalfway", 1), entry("windowClosesOn", "2026-11-05"),
                entry("hottestMax", 36.5), entry("hottestOn", "2026-11-01"), entry("openAlerts", 3),
                entry("plotsWithAlerts", 2), entry("incidentType", "FROST_WARNING"), entry("severity", "CRITICAL"));
        assertThat(facts.facts()).doesNotContainKey("percentToRemove");
        assertThat(sentence(facts, 0)).isEqualTo(CatalogText.of(PREFIX + "week_plan.thinning.open.field", 2, 5,
                TextDate.of(LocalDate.of(2026, 11, 5))));
        assertThat(sentence(facts, 2).key()).isEqualTo(PREFIX + "week_plan.alerts.some.field");
        assertThat(facts.cards().get(1).pill()).isEqualTo(CatalogText.of("assistant.template.card.in_plots.pill", 2));
    }

    @Test
    @DisplayName("week_plan priority: a window opening within 7 days, then a sampling halfway, then nothing")
    void weekPlanPriority() {
        var opening = weekPlan.assemble(onePlot(withPlan(plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20),
                null), 0))).orElseThrow();
        assertThat(sentence(opening, 0)).isEqualTo(CatalogText.of(PREFIX + "week_plan.thinning.opening",
                TextDate.of(LocalDate.of(2026, 11, 2)), 30L));
        assertThat(opening.facts()).containsEntry("windowOpensOn", "2026-11-02");
        // Without forecast the weather sentence and card say so
        assertThat(sentence(opening, 1).key()).isEqualTo(PREFIX + "week_plan.forecast.none");
        assertThat(opening.cards().get(0).value().key()).isEqualTo("assistant.template.card.no_forecast.value");
        assertThat(opening.facts()).doesNotContainKeys("hottestMax", "heatThreshold");

        // 5 Nov is the last day of the week starting 30 Oct; 6 Nov is next week
        var nextWeek = weekPlan.assemble(onePlot(withPlan(plan(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 20),
                null), 0))).orElseThrow();
        assertThat(sentence(nextWeek, 0).key()).isEqualTo(PREFIX + "week_plan.thinning.none");
        var done = weekPlan.assemble(onePlot(withPlan(plan(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 10, 28)), 0))).orElseThrow();
        assertThat(sentence(done, 0).key()).isEqualTo(PREFIX + "week_plan.thinning.none");

        var sampling = weekPlan.assemble(onePlot(withPlan(halfway(), 2))).orElseThrow();
        assertThat(sentence(sampling, 0)).isEqualTo(CatalogText.of(PREFIX + "week_plan.thinning.sampling", 2));
        assertThat(sampling.facts()).containsEntry("treesNeeded", 2);

        var none = weekPlan.assemble(field(bare(), bare())).orElseThrow();
        assertThat(sentence(none, 0)).isEqualTo(CatalogText.of(PREFIX + "week_plan.thinning.none.field", 2));
        assertThat(sentence(none, 2).key()).isEqualTo(PREFIX + "week_plan.alerts.none.field");
    }

    @Test
    @DisplayName("A field of a single active plot reads like that plot; a field without plots has no week plan")
    void fieldOfOnePlot() {
        var facts = weekPlan.assemble(field(withPlan(issuedPlan(), 0))).orElseThrow();

        assertThat(facts.facts()).containsEntry("scope", "FIELD").containsEntry("activePlots", 1)
                .containsEntry("percentToRemove", 30L);
        assertThat(sentence(facts, 0).key()).isEqualTo(PREFIX + "week_plan.thinning.open");
        assertThat(weekPlan.assemble(field())).isEmpty();
    }

    @Test
    @DisplayName("alerts_affecting without alerts says so, for a plot and for the field")
    void noAlerts() {
        var plot = alerts.assemble(onePlot(bare())).orElseThrow();
        assertThat(plot.facts()).containsExactly(entry("openAlerts", 0), entry("plotsWithAlerts", 0),
                entry("scope", "PLOT"));
        assertThat(plot.text().key()).isEqualTo(PREFIX + "alerts_affecting.text.none");
        assertThat(plot.cards().get(1).value().key()).isEqualTo("assistant.template.card.no_alerts.value");

        var field = alerts.assemble(field(bare(), bare())).orElseThrow();
        assertThat(field.text().key()).isEqualTo(PREFIX + "alerts_affecting.text.none.field");
        assertThat(field.cards().get(1).value()).isEqualTo(CatalogText.of("assistant.template.value.integer", 2));
        // A field without plots still answers: there is nothing open
        assertThat(alerts.assemble(field())).isPresent();
    }

    @Test
    @DisplayName("alerts_affecting: the most severe alert against its threshold, the others counted; other types ignored")
    void someAlerts() {
        var plot = alerts.assemble(onePlot(withAlerts(heat(), hydric()))).orElseThrow();
        assertThat(plot.facts()).containsExactly(entry("alertRaisedOn", "2026-10-28"), entry("alertThreshold", 32.0),
                entry("alertValue", 34.2), entry("incidentType", "HEAT_WAVE"), entry("openAlerts", 2),
                entry("plotsWithAlerts", 1), entry("scope", "PLOT"), entry("severity", "WARNING"),
                entry("unit", "°C"));
        assertThat(plot.text().key()).isEqualTo(PREFIX + "alerts_affecting.text.some");
        assertThat(plot.sources()).extracting(CatalogText::key).containsExactly("assistant.template.source.alert");

        var warmWinter = new Incident("WARM_WINTER", "CRITICAL", 26.0, 24.0, "°C", LocalDate.of(2026, 10, 30), 0, 0);
        var field = alerts.assemble(field(withAlerts(heat()), withAlerts(frost(), warmWinter), bare())).orElseThrow();
        assertThat(field.facts()).contains(entry("incidentType", "FROST_WARNING"), entry("severity", "CRITICAL"),
                entry("openAlerts", 2), entry("plotsWithAlerts", 2), entry("activePlots", 3));
        assertThat(field.text().key()).isEqualTo(PREFIX + "alerts_affecting.text.some.field");
        assertThat(field.text().args()).containsSubsequence(2, -2.4).endsWith(2, 3);
        assertThat(field.sources()).extracting(CatalogText::key)
                .containsExactly("assistant.template.source.open_alerts");
    }

    @Test
    @DisplayName("campaign_vs_previous of a plot: last harvest against the one before it, with the bearing index")
    void campaignOfAPlot() {
        var above = campaign.assemble(onePlot(withYields(harvests(2023, 9600, 3100, 8300), 0.5123,
                "MODERATE_ALTERNATION"))).orElseThrow();
        assertThat(above.facts()).containsExactly(entry("bearingBand", "MODERATE_ALTERNATION"),
                entry("bearingIndex", 0.51), entry("comparedPlots", 1), entry("differencePercent", 168L),
                entry("harvestCampaigns", 3), entry("lastCampaignKg", 8300L), entry("lastCampaignYear", 2025),
                entry("previousCampaignKg", 3100L), entry("previousCampaignYear", 2024), entry("scope", "PLOT"),
                entry("trend", "ABOVE"));
        assertThat(above.text().key()).isEqualTo(PREFIX + "campaign_vs_previous.text.above");
        assertThat(((CatalogText) above.text().args().get(5)).key()).isEqualTo(PREFIX + "campaign_vs_previous.bearing");

        var below = campaign.assemble(onePlot(withYields(harvests(2024, 5000, 4000), null, null))).orElseThrow();
        assertThat(below.facts()).containsEntry("trend", "BELOW").containsEntry("differencePercent", 20L)
                .doesNotContainKey("bearingIndex");
        assertThat(((CatalogText) below.text().args().get(5)).key())
                .isEqualTo(PREFIX + "campaign_vs_previous.bearing.missing");
        var equal = campaign.assemble(onePlot(withYields(harvests(2024, 5000, 5000), null, null))).orElseThrow();
        assertThat(equal.text().key()).isEqualTo(PREFIX + "campaign_vs_previous.text.equal");
    }

    @Test
    @DisplayName("campaign_vs_previous of the field: only the plots with both harvests are summed")
    void campaignOfTheField() {
        var facts = campaign.assemble(field(withYields(harvests(2024, 1000, 3000), null, null),
                withYields(harvests(2025, 2000), null, null),
                withYields(harvests(2023, 4000, 4000, 4000), null, null))).orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("activePlots", 3), entry("comparedPlots", 2),
                entry("differencePercent", 40L), entry("lastCampaignKg", 7000L), entry("lastCampaignYear", 2025),
                entry("plotsLeftOut", 1), entry("previousCampaignKg", 5000L), entry("previousCampaignYear", 2024),
                entry("scope", "FIELD"), entry("trend", "ABOVE"));
        assertThat(facts.text().key()).isEqualTo(PREFIX + "campaign_vs_previous.text.above.field");
        assertThat(facts.text().args().get(5)).isEqualTo(CatalogText.of(PREFIX + "campaign_vs_previous.left_out", 1));
        assertThat(facts.sources()).containsExactly(
                CatalogText.of("assistant.template.source.campaigns.field", 2));
    }

    @Test
    @DisplayName("campaign_vs_previous needs two campaigns of the same plots, the first with a harvest")
    void campaignNeedsTwoCampaigns() {
        assertThat(campaign.assemble(onePlot(withYields(harvests(2025, 3000), null, null)))).isEmpty();
        assertThat(campaign.assemble(onePlot(withYields(harvests(2024, 0, 3000), null, null)))).isEmpty();
        // The latest two years of the field are 2025 and 2024: only the second plot has both
        var facts = campaign.assemble(field(withYields(harvests(2020, 1000, 2000), null, null),
                withYields(harvests(2024, 1000, 1500), null, null))).orElseThrow();
        assertThat(facts.facts()).containsEntry("comparedPlots", 1).containsEntry("lastCampaignKg", 1500L)
                .containsEntry("differencePercent", 50L);
    }

    @Test
    @DisplayName("harvest_estimate of a plot: the yield and caliber projected after the confirmed thinning")
    void harvestOfAPlot() {
        var withYield = harvest.assemble(onePlot(withPlan(estimated(8213.6), 0))).orElseThrow();
        assertThat(withYield.facts()).containsExactly(entry("campaignYear", 2026),
                entry("commercialSizeGrade", "171/190"), entry("estimatedKgPerHa", 8214L),
                entry("mostLikelyFruitsPerKg", 180L), entry("scope", "PLOT"),
                entry("thinningRecordedOn", "2026-10-28"));
        assertThat(withYield.text()).isEqualTo(CatalogText.of(PREFIX + "harvest_estimate.text.yield", 8214L, 180L,
                "171/190"));
        assertThat(withYield.sources()).extracting(CatalogText::key)
                .containsExactly("assistant.template.source.thinning");

        var noYield = harvest.assemble(onePlot(withPlan(estimated(null), 0))).orElseThrow();
        assertThat(noYield.text().key()).isEqualTo(PREFIX + "harvest_estimate.text.no_yield");
        assertThat(harvest.assemble(onePlot(withPlan(issuedPlan(), 0)))).isEmpty();
    }

    @Test
    @DisplayName("harvest_estimate of the field: the range of kg per hectare, never a sum")
    void harvestOfTheField() {
        var range = harvest.assemble(field(withPlan(estimated(8213.6), 0), withPlan(estimated(9100.2), 0), bare()))
                .orElseThrow();
        assertThat(range.facts()).containsExactly(entry("activePlots", 3), entry("campaignYear", 2026),
                entry("estimatedPlots", 2), entry("maxKgPerHa", 9100L), entry("minKgPerHa", 8214L),
                entry("plotsWithoutYield", 1), entry("scope", "FIELD"), entry("yieldPlots", 2));
        assertThat(range.text()).isEqualTo(CatalogText.of(PREFIX + "harvest_estimate.text.field.range", 2, 3, 8214L,
                9100L, 1));
        assertThat(range.cards().get(0).value())
                .isEqualTo(CatalogText.of("assistant.template.value.integer_range", 8214L, 9100L));

        var same = harvest.assemble(field(withPlan(estimated(8000.0), 0), withPlan(estimated(8000.4), 0)))
                .orElseThrow();
        assertThat(same.text().key()).isEqualTo(PREFIX + "harvest_estimate.text.field.single");
        assertThat(same.facts()).containsEntry("plotsWithoutYield", 0);

        var noYield = harvest.assemble(field(withPlan(estimated(null), 0), bare())).orElseThrow();
        assertThat(noYield.text()).isEqualTo(CatalogText.of(PREFIX + "harvest_estimate.text.field.no_yield", 1, 2));
        assertThat(harvest.assemble(field(bare(), bare()))).isEmpty();
    }

    private static CatalogText sentence(AssembledFacts facts, int index) {
        return (CatalogText) facts.text().args().get(index);
    }
}
