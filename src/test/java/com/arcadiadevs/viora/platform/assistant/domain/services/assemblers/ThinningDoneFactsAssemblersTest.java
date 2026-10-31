package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.PLOT;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.thinnedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.withPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("THINNING_DONE facts assemblers")
class ThinningDoneFactsAssemblersTest {

    private static final String PREFIX = "assistant.template.thinning.";
    private static final LocalDate EXECUTED = LocalDate.of(2026, 10, 28);
    private final ThinningHarvestChangeFactsAssembler harvest = new ThinningHarvestChangeFactsAssembler();
    private final ThinningWhyTargetFactsAssembler whyTarget = new ThinningWhyTargetFactsAssembler();
    private final ThinningNextSamplingFactsAssembler nextSampling = new ThinningNextSamplingFactsAssembler();

    @Test
    @DisplayName("The three THINNING_DONE questions are registered; nothing is answered before the thinning")
    void registeredAndNeedTheThinning() {
        var registry = FactsAssemblerRegistry.standard();

        assertThat(List.of(QuestionId.THINNING_HARVEST_CHANGE, QuestionId.THINNING_WHY_TARGET,
                QuestionId.THINNING_NEXT_SAMPLING))
                .allSatisfy(id -> assertThat(registry.find(id)).map(FactsAssembler::questionId).contains(id));
        assertThat(List.<FactsAssembler>of(harvest, whyTarget, nextSampling))
                .allSatisfy(assembler -> assertThat(assembler.assemble(done(null))).isEmpty());
    }

    @Test
    @DisplayName("harvest_change with the contract figure: 8.200 kg/ha against without thinning, load balanced")
    void harvestChangeCompared() {
        var facts = harvest.assemble(done(outcome("CALIBER_ADJUSTED", 9100.0, "BALANCED", "AGRONOMIST_APPROVED")))
                .orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("estimatedKgPerHa", 8200L), entry("harvestCampaigns", 3),
                entry("kgPerHaWithoutThinning", 9100L), entry("loadState", "BALANCED"),
                entry("residualLoad", 0.4), entry("targetLoad", 0.4), entry("yieldBasis", "CALIBER_ADJUSTED"));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "harvest_change.text",
                CatalogText.of(PREFIX + "harvest_change.yield.compared", 8200L, 9100L),
                CatalogText.of(PREFIX + "harvest_change.load.BALANCED", 0.4, 0.4)));
        assertThat(facts.cards().get(0).pill().key()).isEqualTo("assistant.template.card.calibrating.pill");
        assertThat(facts.sources()).containsExactly(
                CatalogText.of("assistant.template.source.thinning", TextDate.of(EXECUTED)),
                CatalogText.of("assistant.template.source.campaigns", 3));
    }

    @Test
    @DisplayName("harvest_change counting fruits only: the yield without thinning is a ceiling; never about 2027")
    void harvestChangeCeiling() {
        var facts = harvest.assemble(done(outcome("FRUIT_COUNT_ONLY", 11700.0, "SEVERE_OVERLOAD", null)))
                .orElseThrow();

        assertThat(facts.text().args()).containsExactly(
                CatalogText.of(PREFIX + "harvest_change.yield.ceiling", 8200L, 11700L),
                CatalogText.of(PREFIX + "harvest_change.load.SEVERE_OVERLOAD", 0.4, 0.4));
        assertThat(facts.cards().get(0).pill().key()).isEqualTo("assistant.template.card.fruit_count.pill");
        assertThat(facts.facts()).doesNotContainKeys("nextCampaignYear", "bearingIndex");

        var only = harvest.assemble(done(new ThinningOutcomeData(EXECUTED, null, null, null, null, null, null, null,
                8200.0, "CALIBER_ADJUSTED", 3, null, null, null))).orElseThrow();
        assertThat(only.text()).isEqualTo(CatalogText.of(PREFIX + "harvest_change.text.single",
                CatalogText.of(PREFIX + "harvest_change.yield.only", 8200L)));
        assertThat(harvest.assemble(done(new ThinningOutcomeData(EXECUTED, 30.0, null, 0.57, 0.4, 0.4, "BALANCED",
                null, null, null, 0, null, null, null)))).isEmpty();
    }

    @Test
    @DisplayName("why_target: the target of the variety profile, who approved it, and where the thinning left it")
    void whyTarget() {
        var facts = whyTarget.assemble(done(outcome("CALIBER_ADJUSTED", 9100.0, "BALANCED", "SYNTHETIC_DEMO")))
                .orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("preThinningLoad", 0.57), entry("profileStatus",
                "SYNTHETIC_DEMO"), entry("removedPercent", 30L), entry("residualLoad", 0.4), entry("targetLoad", 0.4));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "why_target.text",
                CatalogText.of(PREFIX + "why_target.target", 0.4),
                CatalogText.of(PREFIX + "why_target.profile.SYNTHETIC_DEMO"),
                CatalogText.of(PREFIX + "why_target.result", 0.57, 30L, 0.4)));
        assertThat(facts.cards().get(0).pill().key())
                .isEqualTo("assistant.template.card.profile.pill.SYNTHETIC_DEMO");

        var unknown = whyTarget.assemble(done(new ThinningOutcomeData(EXECUTED, null, null, null, null, 0.4, null,
                null, null, null, 0, "SOMETHING_ELSE", null, null))).orElseThrow();
        assertThat(unknown.text()).isEqualTo(CatalogText.of(PREFIX + "why_target.text.short",
                CatalogText.of(PREFIX + "why_target.target", 0.4),
                CatalogText.of(PREFIX + "why_target.profile.unknown")));
        assertThat(unknown.facts()).doesNotContainKey("profileStatus");
    }

    @Test
    @DisplayName("next_sampling: no more this campaign; next campaign after full bloom, within the profile window")
    void nextSampling() {
        var facts = nextSampling.assemble(done(outcome("CALIBER_ADJUSTED", 9100.0, "BALANCED", null))).orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("nextCampaignYear", 2027),
                entry("thinningRecordedOn", "2026-10-28"), entry("windowClosesDaysAfterBloom", 49),
                entry("windowOpensDaysAfterBloom", 14));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "next_sampling.text.window",
                TextDate.of(EXECUTED), "2027", 14, 49));
        assertThat(facts.cards().get(1).value())
                .isEqualTo(CatalogText.of("assistant.template.value.integer_range", 14, 49));

        var withoutOffsets = nextSampling.assemble(done(new ThinningOutcomeData(EXECUTED, null, null, null, null,
                null, null, null, null, null, 0, null, null, null))).orElseThrow();
        assertThat(withoutOffsets.text()).isEqualTo(CatalogText.of(PREFIX + "next_sampling.text",
                TextDate.of(EXECUTED), "2027"));
    }

    private static ThinningOutcomeData outcome(String basis, Double without, String loadState, String profile) {
        return new ThinningOutcomeData(EXECUTED, 30.0, "OPTIMAL", 0.5714, 0.4, 0.4, loadState, without, 8200.0, basis,
                3, profile, 14, 49);
    }

    private static PlotAnswerData done(ThinningOutcomeData outcome) {
        return PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026,
                new HomeData(false, List.of(withPlan(thinnedPlan(8200.0), 0)), THRESHOLDS).withOutcome(outcome));
    }
}
