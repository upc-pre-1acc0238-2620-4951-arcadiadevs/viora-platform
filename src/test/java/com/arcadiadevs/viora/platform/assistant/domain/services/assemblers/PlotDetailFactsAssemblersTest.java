package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Forecast;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Incident;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData.PlotHome;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.PLOT;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.plan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.thinnedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.yields;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.forecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.frost;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.hotThenMild;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.bare;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.halfway;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.withPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("PLOT_DETAIL facts assemblers")
class PlotDetailFactsAssemblersTest {

    private static final String PREFIX = "assistant.template.plot.";
    private final PlotHowIsFactsAssembler howIs = new PlotHowIsFactsAssembler();
    private final PlotTodayFactsAssembler today = new PlotTodayFactsAssembler();
    private final PlotLastSamplingFactsAssembler lastSampling = new PlotLastSamplingFactsAssembler();
    private final NextYearFactsAssembler nextYear = new NextYearFactsAssembler(QuestionId.PLOT_NEXT_YEAR_HARVEST);

    @Test
    @DisplayName("The four PLOT_DETAIL questions are registered, one assembler each")
    void registered() {
        var registry = FactsAssemblerRegistry.standard();

        assertThat(List.of(QuestionId.PLOT_HOW_IS, QuestionId.PLOT_TODAY, QuestionId.PLOT_LAST_SAMPLING,
                QuestionId.PLOT_NEXT_YEAR_HARVEST))
                .allSatisfy(id -> assertThat(registry.find(id)).map(FactsAssembler::questionId).contains(id));
        assertThat(registry.find(QuestionId.ALTERNATION_NEXT_YEAR)).map(FactsAssembler::questionId)
                .contains(QuestionId.ALTERNATION_NEXT_YEAR);
    }

    @Test
    @DisplayName("Data of another screen, or of the whole field, never answers a PLOT_DETAIL question")
    void onlyOnePlot() {
        var field = PlotAnswerData.ofHome(null, TODAY, 2026,
                new HomeData(true, List.of(bare(), bare()), THRESHOLDS));

        for (var other : List.of(AnswerDataSamples.plan(issuedPlan()), field)) {
            assertThat(List.<FactsAssembler>of(howIs, today, lastSampling))
                    .allSatisfy(assembler -> assertThat(assembler.assemble(other)).isEmpty());
        }
        assertThatThrownBy(() -> PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026,
                new HomeData(false, List.of(), THRESHOLDS)))
                .hasMessage("assistant.answer_data.invalid");
    }

    @Test
    @DisplayName("how_is with the contract example: bearing index 0,51 and 12 days left in the open window")
    void howIsContractExample() {
        var facts = howIs.assemble(plot(issuedPlan(), 0, List.of(), null)).orElseThrow();

        assertThat(facts.facts()).containsExactly(
                entry("bearingBand", "MODERATE_ALTERNATION"), entry("bearingIndex", 0.51), entry("daysLeft", 12L),
                entry("harvestCampaigns", 3), entry("percentToRemove", 30L), entry("windowClosesOn", "2026-11-10"),
                entry("windowState", "OPEN"));
        assertThat(facts.text().key()).isEqualTo(PREFIX + "how_is.text");
        assertThat(sentence(facts, 0)).isEqualTo(CatalogText.of(PREFIX + "how_is.index", 0.51,
                CatalogText.of("assistant.template.band.MODERATE_ALTERNATION")));
        assertThat(sentence(facts, 1)).isEqualTo(CatalogText.of(PREFIX + "how_is.thinning.open", 12L, 30L,
                TextDate.of(LocalDate.of(2026, 11, 10))));
        assertThat(facts.cards().get(0).value()).isEqualTo(CatalogText.of("assistant.template.value.decimal", 0.51));
        assertThat(facts.cards().get(1).value()).isEqualTo(CatalogText.of("assistant.template.value.integer", 12L));
        assertThat(facts.cards().get(1).label())
                .isEqualTo(CatalogText.of("assistant.template.card.days_left.label", 12L));
        assertThat(facts.cards().get(1).pill().key()).isEqualTo("assistant.template.card.window.open");
        assertThat(facts.sources()).extracting(CatalogText::key).containsExactly(
                "assistant.template.source.campaigns", "assistant.template.source.sampling");
    }

    @Test
    @DisplayName("how_is states: window to open, closed, thinning recorded, sampling halfway, without index")
    void howIsStates() {
        var before = howIs.assemble(plot(plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20), null), 0,
                List.of(), null)).orElseThrow();
        assertThat(before.facts()).contains(entry("windowState", "BEFORE"), entry("daysUntilOpen", 3L),
                entry("windowOpensOn", "2026-11-02"));
        assertThat(sentence(before, 1).key()).isEqualTo(PREFIX + "how_is.thinning.before");

        var closed = howIs.assemble(plot(plan(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), null), 0,
                List.of(), null)).orElseThrow();
        assertThat(closed.facts()).contains(entry("windowState", "CLOSED"));
        assertThat(sentence(closed, 1).key()).isEqualTo(PREFIX + "how_is.thinning.closed");

        var done = howIs.assemble(plot(thinnedPlan(null), 0, List.of(), null)).orElseThrow();
        assertThat(done.facts()).contains(entry("windowState", "DONE"), entry("thinningRecordedOn", "2026-10-28"));
        assertThat(done.cards().get(1).value().key()).isEqualTo("assistant.template.card.done.value");

        var sampling = howIs.assemble(PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026,
                new HomeData(false, List.of(withPlan(halfway(), 2)), THRESHOLDS))).orElseThrow();
        assertThat(sampling.facts()).containsExactly(entry("sampledTrees", 3), entry("treesNeeded", 2),
                entry("windowState", "SAMPLING"));
        assertThat(sentence(sampling, 0).key()).isEqualTo(PREFIX + "how_is.index.none");
        assertThat(sentence(sampling, 1)).isEqualTo(CatalogText.of(PREFIX + "how_is.thinning.sampling", 2));
        assertThat(sampling.cards().get(0).value().key()).isEqualTo("assistant.template.card.no_index.value");

        // Neither index nor sampling: nothing to answer, the catalog disables it
        assertThat(howIs.assemble(PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026,
                new HomeData(false, List.of(bare()), THRESHOLDS)))).isEmpty();
    }

    @Test
    @DisplayName("today: open window, today's heat against the threshold and the open alert")
    void todayWithWorkToDo() {
        var facts = today.assemble(plot(issuedPlan(), 0, List.of(heat()), hotThenMild())).orElseThrow();

        assertThat(facts.facts()).containsExactly(
                entry("daysLeft", 12L), entry("frostReached", false), entry("frostThreshold", 1.5),
                entry("heatReached", true), entry("heatThreshold", 32.0), entry("incidentType", "HEAT_WAVE"),
                entry("openAlerts", 1), entry("percentToRemove", 30L), entry("severity", "WARNING"),
                entry("todayMax", 34.2), entry("todayMin", 14.0), entry("windowClosesOn", "2026-11-10"),
                entry("windowState", "OPEN"));
        assertThat(sentence(facts, 0)).isEqualTo(CatalogText.of(PREFIX + "today.thinning.open",
                TextDate.of(LocalDate.of(2026, 11, 10)), 30L));
        assertThat(sentence(facts, 1)).isEqualTo(CatalogText.of(PREFIX + "today.forecast.heat", 34.2, 32.0));
        assertThat(sentence(facts, 2).key()).isEqualTo(PREFIX + "today.alerts.some");
        assertThat(facts.cards().get(0).label().key()).isEqualTo("assistant.template.card.today_max.label");
        assertThat(facts.cards().get(0).pill()).isEqualTo(CatalogText.of("assistant.template.card.today_min.pill",
                14.0));
        assertThat(facts.sources()).extracting(CatalogText::key).containsExactly(
                "assistant.template.source.plan", "assistant.template.source.forecast",
                "assistant.template.source.open_alerts.plot");
    }

    @Test
    @DisplayName("today without anything pending: no thinning task, frost or mild day, no forecast, no alerts")
    void todayQuiet() {
        var frosty = today.assemble(plot(null, 0, List.of(frost()), forecast(8.0))).orElseThrow();
        assertThat(sentence(frosty, 0).key()).isEqualTo(PREFIX + "today.thinning.none");
        assertThat(sentence(frosty, 1)).isEqualTo(CatalogText.of(PREFIX + "today.forecast.frost", 0.0, 1.5));
        assertThat(frosty.facts()).contains(entry("windowState", "NONE"), entry("frostReached", true));
        assertThat(frosty.sources()).extracting(CatalogText::key).doesNotContain("assistant.template.source.plan");

        var mild = today.assemble(plot(null, 0, List.of(), forecast(25.0))).orElseThrow();
        assertThat(sentence(mild, 1)).isEqualTo(CatalogText.of(PREFIX + "today.forecast.mild", 14.0, 25.0));
        assertThat(sentence(mild, 2).key()).isEqualTo(PREFIX + "today.alerts.none");

        var unforecast = today.assemble(plot(thinnedPlan(null), 0, List.of(), null)).orElseThrow();
        assertThat(sentence(unforecast, 0).key()).isEqualTo(PREFIX + "today.thinning.none");
        assertThat(sentence(unforecast, 1).key()).isEqualTo(PREFIX + "today.forecast.none");
        assertThat(unforecast.cards().get(0).value().key()).isEqualTo("assistant.template.card.no_forecast.value");

        var halfwaySampling = today.assemble(PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026,
                new HomeData(false, List.of(withPlan(halfway(), 1)), THRESHOLDS))).orElseThrow();
        assertThat(sentence(halfwaySampling, 0)).isEqualTo(CatalogText.of(PREFIX + "today.thinning.sampling", 1));
    }

    @Test
    @DisplayName("last_sampling with the prescription: trees, latest round, load against the target and percent")
    void lastSamplingIssued() {
        var facts = lastSampling.assemble(plot(issuedPlan(), 0, List.of(), null)).orElseThrow();

        assertThat(facts.facts()).containsExactly(
                entry("lastSampledOn", "2026-10-29"), entry("percentToRemove", 30L), entry("preThinningLoad", 0.57),
                entry("prescriptionIssued", true), entry("sampledTrees", 5), entry("targetLoad", 0.4));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "last_sampling.text.issued", 5,
                CatalogText.of(PREFIX + "last_sampling.latest", TextDate.of(LocalDate.of(2026, 10, 29))),
                0.57, 0.4, 30L));
        assertThat(facts.cards().get(0).label())
                .isEqualTo(CatalogText.of("assistant.template.card.sampled_trees.label", 5));
        assertThat(facts.cards().get(1).pill()).isEqualTo(CatalogText.of("assistant.template.card.target.pill", 0.4));
        assertThat(facts.sources()).containsExactly(CatalogText.of("assistant.template.source.sampling", 5,
                TextDate.of(LocalDate.of(2026, 10, 29))));
    }

    @Test
    @DisplayName("last_sampling before the prescription: the trees still missing; without sampling, nothing")
    void lastSamplingHalfway() {
        var facts = lastSampling.assemble(PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026,
                new HomeData(false, List.of(withPlan(halfway(), 2)), THRESHOLDS))).orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("lastSampledOn", "2026-10-29"),
                entry("prescriptionIssued", false), entry("sampledTrees", 3), entry("treesNeeded", 2));
        assertThat(facts.text().key()).isEqualTo(PREFIX + "last_sampling.text.halfway");
        assertThat(facts.cards().get(1).label())
                .isEqualTo(CatalogText.of("assistant.template.card.trees_needed.label", 2));

        var undated = new ThinningPlanData(false, null, null, null, null, null, 4, null, null, null, null, null,
                null);
        var pending = lastSampling.assemble(PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026,
                new HomeData(false, List.of(withPlan(undated, 0)), THRESHOLDS))).orElseThrow();
        assertThat(pending.text()).isEqualTo(CatalogText.of(PREFIX + "last_sampling.text.pending", 4,
                CatalogText.of(PREFIX + "last_sampling.latest.undated")));
        assertThat(pending.sources())
                .containsExactly(CatalogText.of("assistant.template.source.sampling_undated", 4));

        assertThat(lastSampling.assemble(plot(null, 0, List.of(), null))).isEmpty();
    }

    @Test
    @DisplayName("next_year_harvest answers as alternation.next_year, with its own texts; nothing without caliber")
    void nextYearHarvest() {
        var facts = nextYear.assemble(plot(thinnedPlan(14250.0), 0, List.of(), null)).orElseThrow();

        assertThat(facts.facts()).containsExactly(
                entry("campaignYear", 2026), entry("commercialSizeGrade", "171/190"), entry("estimatedKgPerHa", 14250L),
                entry("harvestCampaigns", 3), entry("mostLikelyFruitsPerKg", 180L),
                entry("thinningRecordedOn", "2026-10-28"));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "next_year_harvest.text.yield", 14250L, 180L,
                "171/190"));
        assertThat(nextYear.assemble(plot(issuedPlan(), 0, List.of(), null))).isEmpty();
    }

    /** PLOT_DETAIL about the sample plot, with index 0,5123 (moderate alternation) and three harvests. */
    private static PlotAnswerData plot(ThinningPlanData plan, int treesNeeded, List<Incident> alerts,
            Forecast forecast) {
        return PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026, new HomeData(false, List.of(new PlotHome(plan,
                treesNeeded, alerts, yields(), 0.5123, "MODERATE_ALTERNATION", forecast)), THRESHOLDS));
    }

    private static CatalogText sentence(AssembledFacts facts, int index) {
        return (CatalogText) facts.text().args().get(index);
    }
}
