package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ALTERNATION facts assemblers")
class AlternationFactsAssemblersTest {

    @Nested
    @DisplayName("alternation.why_index")
    class WhyIndex {

        private final WhyIndexFactsAssembler assembler = new WhyIndexFactsAssembler();

        @Test
        @DisplayName("Facts: the index rounded as shown, its band and the campaigns behind it")
        void assemblesTheIndex() {
            var facts = assembler.assemble(alternation(issuedPlan())).orElseThrow();

            assertThat(assembler.questionId()).isEqualTo(QuestionId.ALTERNATION_WHY_INDEX);
            assertThat(facts.facts()).containsEntry("bearingIndex", 0.51)
                    .containsEntry("bearingBand", "MODERATE_ALTERNATION")
                    .containsEntry("harvestCampaigns", 3)
                    .containsEntry("lastCampaignYear", 2025)
                    .containsEntry("lastCampaignKg", 8300L);
            assertThat(facts.text().key()).isEqualTo("assistant.template.alternation.why_index.text");
            assertTwoToneCards(facts);
            assertThat(facts.cards().get(0).value()).isEqualTo(CatalogText.of("assistant.template.value.decimal", 0.51));
            assertThat(facts.sources()).containsExactly(CatalogText.of("assistant.template.source.campaigns", 3));
            assertThat(facts.actions()).isEmpty();
        }

        @Test
        @DisplayName("Without an index there is nothing to explain")
        void unavailableWithoutIndex() {
            assertThat(assembler.assemble(withoutIndex())).isEmpty();
        }
    }

    @Nested
    @DisplayName("alternation.break_cycle")
    class BreakCycle {

        private final BreakCycleFactsAssembler assembler = new BreakCycleFactsAssembler();

        @Test
        @DisplayName("With an issued plan: the index and the prescribed percentage")
        void withAnIssuedPlan() {
            var facts = assembler.assemble(alternation(issuedPlan())).orElseThrow();

            assertThat(assembler.questionId()).isEqualTo(QuestionId.ALTERNATION_BREAK_CYCLE);
            assertThat(facts.facts()).containsEntry("bearingIndex", 0.51).containsEntry("prescriptionIssued", true)
                    .containsEntry("percentToRemove", 30);
            assertThat(facts.text().key()).isEqualTo("assistant.template.alternation.break_cycle.text.plan");
            assertTwoToneCards(facts);
            assertThat(facts.actions()).isEmpty();
        }

        @Test
        @DisplayName("Without a plan: the index and a call to sample, no percentage")
        void withoutPlan() {
            var facts = assembler.assemble(alternation((com.arcadiadevs.viora.platform.assistant.domain.model
                    .valueobjects.ThinningPlanData) null)).orElseThrow();

            assertThat(facts.facts()).containsEntry("prescriptionIssued", false).doesNotContainKey("percentToRemove");
            assertThat(facts.text().key()).isEqualTo("assistant.template.alternation.break_cycle.text.no_plan");
            assertTwoToneCards(facts);
        }

        @Test
        @DisplayName("Without an index it is unavailable")
        void unavailableWithoutIndex() {
            assertThat(assembler.assemble(withoutIndex())).isEmpty();
        }
    }

    @Nested
    @DisplayName("alternation.vs_campaigns")
    class VsCampaigns {

        private final VsCampaignsFactsAssembler assembler = new VsCampaignsFactsAssembler();

        @Test
        @DisplayName("Compares the last campaign with the mean of the earlier ones: 8300 vs 6350 is 31 % above")
        void comparesWithTheAverage() {
            var facts = assembler.assemble(alternation(issuedPlan())).orElseThrow();

            assertThat(assembler.questionId()).isEqualTo(QuestionId.ALTERNATION_VS_CAMPAIGNS);
            assertThat(facts.facts()).containsEntry("lastCampaignYear", 2025).containsEntry("lastCampaignKg", 8300L)
                    .containsEntry("previousCampaigns", 2).containsEntry("previousAverageKg", 6350L)
                    .containsEntry("differencePercent", 31L).containsEntry("trend", "ABOVE");
            assertThat(facts.text().key()).isEqualTo("assistant.template.alternation.vs_campaigns.text.above");
            assertTwoToneCards(facts);
        }

        @Test
        @DisplayName("A lower campaign reads below the average")
        void belowTheAverage() {
            var yields = new TreeMap<Integer, Double>();
            yields.put(2024, 9600.0);
            yields.put(2025, 3100.0);

            var facts = assembler.assemble(alternation(yields)).orElseThrow();

            assertThat(facts.facts()).containsEntry("differencePercent", 68L).containsEntry("trend", "BELOW");
            assertThat(facts.text().key()).isEqualTo("assistant.template.alternation.vs_campaigns.text.below");
        }

        @Test
        @DisplayName("With fewer than two campaigns it is unavailable")
        void unavailableWithOneCampaign() {
            var yields = new TreeMap<Integer, Double>();
            yields.put(2025, 3100.0);

            assertThat(assembler.assemble(alternation(yields))).isEmpty();
        }
    }

    @Nested
    @DisplayName("alternation.next_year")
    class NextYear {

        private final NextYearFactsAssembler assembler = new NextYearFactsAssembler();

        @Test
        @DisplayName("The yield and caliber estimated after the confirmed thinning")
        void withTheYieldAfterThinning() {
            var facts = assembler.assemble(alternation(thinnedPlan(8213.6))).orElseThrow();

            assertThat(assembler.questionId()).isEqualTo(QuestionId.ALTERNATION_NEXT_YEAR);
            assertThat(facts.facts()).containsEntry("estimatedKgPerHa", 8214L)
                    .containsEntry("mostLikelyFruitsPerKg", 180L).containsEntry("commercialSizeGrade", "171/190")
                    .containsEntry("thinningRecordedOn", "2026-10-28");
            assertThat(facts.text().key()).isEqualTo("assistant.template.alternation.next_year.text.yield");
            assertTwoToneCards(facts);
        }

        @Test
        @DisplayName("Without a yield estimate it speaks only of the caliber")
        void withoutYield() {
            var facts = assembler.assemble(alternation(thinnedPlan(null))).orElseThrow();

            assertThat(facts.facts()).doesNotContainKey("estimatedKgPerHa");
            assertThat(facts.text().key()).isEqualTo("assistant.template.alternation.next_year.text.no_yield");
            assertTwoToneCards(facts);
        }

        @Test
        @DisplayName("Without an estimated caliber it is unavailable")
        void unavailableWithoutCaliber() {
            assertThat(assembler.assemble(alternation(issuedPlan()))).isEmpty();
        }
    }

    private static PlotAnswerData withoutIndex() {
        return new PlotAnswerData(PLOT, TODAY, 2026, null, null, yields(), issuedPlan());
    }

    static void assertTwoToneCards(AssembledFacts facts) {
        assertThat(facts.cards()).hasSize(2);
        assertThat(facts.cards()).extracting(FactCardTemplate::tone)
                .containsExactly(FactTone.PRIMARY, FactTone.HIGHLIGHT);
    }
}
