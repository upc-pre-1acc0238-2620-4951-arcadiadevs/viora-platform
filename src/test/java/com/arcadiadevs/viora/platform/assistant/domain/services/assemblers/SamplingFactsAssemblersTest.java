package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ActionTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.PLOT;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.plan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.thinnedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.halfway;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.withPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("SAMPLING_SUMMARY facts assemblers")
class SamplingFactsAssemblersTest {

    private static final String PREFIX = "assistant.template.sampling.";
    private final SamplingEnoughTreesFactsAssembler enoughTrees = new SamplingEnoughTreesFactsAssembler();
    private final SamplingMeaningFactsAssembler meaning = new SamplingMeaningFactsAssembler();
    private final SamplingNextFactsAssembler next = new SamplingNextFactsAssembler();

    @Test
    @DisplayName("enough_trees, meaning and next are registered; correct_tree has no assembler (always disabled)")
    void registered() {
        var registry = FactsAssemblerRegistry.standard();

        assertThat(List.of(QuestionId.SAMPLING_ENOUGH_TREES, QuestionId.SAMPLING_MEANING, QuestionId.SAMPLING_NEXT))
                .allSatisfy(id -> assertThat(registry.find(id)).map(FactsAssembler::questionId).contains(id));
        assertThat(registry.find(QuestionId.SAMPLING_CORRECT_TREE)).isEmpty();
    }

    @Test
    @DisplayName("The minimum shown is the one Thinning uses for a representative sampling")
    void minimumMatchesThinning() {
        assertThat(SamplingEnoughTreesFactsAssembler.MIN_REPRESENTATIVE_TREES)
                .isEqualTo(SamplingCoverageEvaluator.MIN_REPRESENTATIVE_TREES);
    }

    @Test
    @DisplayName("enough_trees with the contract example: 5/5 and the load against the target 0,40")
    void enoughTreesContractExample() {
        var facts = enoughTrees.assemble(sampling(new ThinningPlanData(true, 32.0, 0.59, 0.4, TODAY.minusDays(10),
                TODAY.plusDays(11), 5, LocalDate.of(2026, 10, 29), null, null, null, null, null), 0)).orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("minimumTrees", 5), entry("preThinningLoad", 0.59),
                entry("representative", true), entry("sampledTrees", 5), entry("targetLoad", 0.4),
                entry("treesNeeded", 0));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "enough_trees.text.enough", 5, 5, 0.59, 0.4));
        assertThat(facts.cards().get(0).value()).isEqualTo(CatalogText.of("assistant.template.value.fraction", 5, 5));
        assertThat(facts.cards().get(0).pill().key()).isEqualTo("assistant.template.card.representative.pill");
        assertThat(facts.cards().get(1).pill()).isEqualTo(CatalogText.of("assistant.template.card.target.pill", 0.4));
        assertThat(facts.sources()).containsExactly(CatalogText.of("assistant.template.source.sampling", 5,
                TextDate.of(LocalDate.of(2026, 10, 29))));
    }

    @Test
    @DisplayName("enough_trees short of the minimum gives the trees missing; without sampling, nothing")
    void enoughTreesShort() {
        var facts = enoughTrees.assemble(sampling(halfway(), 2)).orElseThrow();

        assertThat(facts.facts()).contains(entry("representative", false), entry("treesNeeded", 2))
                .doesNotContainKey("preThinningLoad");
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "enough_trees.text.short", 3, 5, 2));
        assertThat(facts.cards().get(0).pill())
                .isEqualTo(CatalogText.of("assistant.template.card.trees_missing.pill", 2));
        assertThat(enoughTrees.assemble(sampling(null, 0))).isEmpty();
    }

    @Test
    @DisplayName("meaning: load over the target, how many times, and the percent; nothing before the prescription")
    void meaningOverTarget() {
        var facts = meaning.assemble(sampling(issuedPlan(), 0)).orElseThrow();

        assertThat(facts.facts()).containsExactly(entry("loadRatio", 1.43), entry("percentToRemove", 30L),
                entry("preThinningLoad", 0.57), entry("sampledTrees", 5), entry("targetLoad", 0.4));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "meaning.text.over", 0.57, 0.4, 1.43, 30L));
        assertThat(facts.cards().get(1).value()).isEqualTo(CatalogText.of("assistant.template.value.percent", 30L));

        var within = meaning.assemble(sampling(new ThinningPlanData(true, 0.0, 0.38, 0.4, TODAY, TODAY.plusDays(5),
                5, TODAY, null, null, null, null, null), 0)).orElseThrow();
        assertThat(within.text().key()).isEqualTo(PREFIX + "meaning.text.within");
        assertThat(meaning.assemble(sampling(halfway(), 2))).isEmpty();
    }

    @Test
    @DisplayName("next with the window open: thin within the days left, with the Registrar aclareo action")
    void nextOpenWindow() {
        var facts = next.assemble(sampling(issuedPlan(), 0)).orElseThrow();

        assertThat(facts.facts()).contains(entry("windowState", "OPEN"), entry("daysLeft", 12L));
        assertThat(facts.text()).isEqualTo(CatalogText.of(PREFIX + "next.text.open", 12L,
                TextDate.of(LocalDate.of(2026, 11, 10)), 30L));
        assertThat(facts.actions()).containsExactly(new ActionTemplate(ActionTemplate.DEEPLINK,
                CatalogText.of("assistant.template.action.register_thinning"),
                "thinning/execute?plotId=" + PLOT.plotId()));
        assertThat(facts.cards().get(1).pill().key()).isEqualTo("assistant.template.card.window.open");
    }

    @Test
    @DisplayName("next in the other states: sample more, plan pending, window to open, closed, recorded")
    void nextStates() {
        assertThat(next.assemble(sampling(halfway(), 2)).orElseThrow().text())
                .isEqualTo(CatalogText.of(PREFIX + "next.text.sample", 2, 5));
        var unissued = new ThinningPlanData(false, null, 0.57, null, null, null, 5, TODAY, null, null, null, null,
                null);
        assertThat(next.assemble(sampling(unissued, 0)).orElseThrow().text().key())
                .isEqualTo(PREFIX + "next.text.unissued");
        var before = next.assemble(sampling(plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20), null), 0))
                .orElseThrow();
        assertThat(before.text().key()).isEqualTo(PREFIX + "next.text.before");
        assertThat(before.actions()).isEmpty();
        assertThat(next.assemble(sampling(plan(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), null), 0))
                .orElseThrow().text().key()).isEqualTo(PREFIX + "next.text.closed");
        assertThat(next.assemble(sampling(thinnedPlan(null), 0)).orElseThrow().text())
                .isEqualTo(CatalogText.of(PREFIX + "next.text.done", TextDate.of(LocalDate.of(2026, 10, 28))));
        assertThat(next.assemble(sampling(null, 0))).isEmpty();
    }

    private static PlotAnswerData sampling(ThinningPlanData plan, int treesNeeded) {
        return PlotAnswerData.ofOnePlot(PLOT, TODAY, 2026,
                new HomeData(false, List.of(withPlan(plan, treesNeeded)), THRESHOLDS));
    }
}
