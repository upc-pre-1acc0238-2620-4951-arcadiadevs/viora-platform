package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ActionTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AlternationFactsAssemblersTest.assertTwoToneCards;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PLAN facts assemblers")
class PlanFactsAssemblersTest {

    private static final String DEEPLINK = "thinning/execute?plotId=" + PLOT.plotId();

    @Nested
    @DisplayName("plan.why_percent")
    class WhyPercent {

        private final WhyPercentFactsAssembler assembler = new WhyPercentFactsAssembler();

        @Test
        @DisplayName("Facts: load, target, percentage, open window with its days left, sampling; register action")
        void assemblesThePlan() {
            var facts = assembler.assemble(plan(issuedPlan())).orElseThrow();

            assertThat(assembler.questionId()).isEqualTo(QuestionId.PLAN_WHY_PERCENT);
            assertThat(facts.facts()).containsEntry("preThinningLoad", 0.57).containsEntry("targetLoad", 0.4)
                    .containsEntry("percentToRemove", 30).containsEntry("windowState", "OPEN")
                    .containsEntry("windowOpensOn", "2026-10-20").containsEntry("windowClosesOn", "2026-11-10")
                    .containsEntry("daysLeft", 12L).containsEntry("sampledTrees", 5)
                    .containsEntry("lastSampledOn", "2026-10-29");
            assertThat(facts.text().key()).isEqualTo("assistant.template.plan.why_percent.text");
            assertTwoToneCards(facts);
            assertThat(facts.sources()).containsExactly(CatalogText.of("assistant.template.source.sampling", 5,
                    TextDate.of(LocalDate.of(2026, 10, 29))));
            assertThat(facts.actions()).containsExactly(new ActionTemplate(ActionTemplate.DEEPLINK,
                    CatalogText.of("assistant.template.action.register_thinning"), DEEPLINK));
        }

        @Test
        @DisplayName("No action once the window closed or the thinning was recorded")
        void noActionOutsideTheWindow() {
            var closed = assembler.assemble(plan(plan(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), null)))
                    .orElseThrow();
            var done = assembler.assemble(plan(plan(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10),
                    LocalDate.of(2026, 10, 28)))).orElseThrow();

            assertThat(closed.facts()).containsEntry("windowState", "CLOSED").doesNotContainKey("daysLeft");
            assertThat(closed.actions()).isEmpty();
            assertThat(done.facts()).containsEntry("windowState", "DONE").containsEntry("thinningRecordedOn",
                    "2026-10-28");
            assertThat(done.actions()).isEmpty();
        }

        @Test
        @DisplayName("Without an issued plan, or a missing load, it is unavailable")
        void unavailableWithoutPlan() {
            assertThat(assembler.assemble(plan((ThinningPlanData) null))).isEmpty();
            assertThat(assembler.assemble(plan(new ThinningPlanData(false, null, 0.57, 0.4, null, null, 5, null, null,
                    null, null, null, null)))).isEmpty();
            assertThat(assembler.assemble(plan(new ThinningPlanData(true, 30.0, null, 0.4, LocalDate.of(2026, 10, 20),
                    LocalDate.of(2026, 11, 10), 5, null, null, null, null, null, null)))).isEmpty();
        }
    }

    @Nested
    @DisplayName("plan.if_less")
    class IfLess {

        private final IfLessFactsAssembler assembler = new IfLessFactsAssembler();

        @Test
        @DisplayName("Load left with the plan (0.40) and with 10 points less (0.46), from the measured 0.5714")
        void comparesBothScenarios() {
            var facts = assembler.assemble(plan(issuedPlan())).orElseThrow();

            assertThat(assembler.questionId()).isEqualTo(QuestionId.PLAN_IF_LESS);
            assertThat(facts.facts()).containsEntry("lessPercent", 20).containsEntry("loadAfterPlan", 0.4)
                    .containsEntry("loadAfterLess", 0.46);
            assertThat(facts.text().key()).isEqualTo("assistant.template.plan.if_less.text");
            assertTwoToneCards(facts);
            assertThat(facts.actions()).isEmpty();
        }

        @Test
        @DisplayName("A plan of 10 % or less has no lighter scenario")
        void minimalPlan() {
            var light = new ThinningPlanData(true, 8.0, 0.43, 0.4, LocalDate.of(2026, 10, 20),
                    LocalDate.of(2026, 11, 10), 5, null, null, null, null, null, null);

            var facts = assembler.assemble(plan(light)).orElseThrow();

            assertThat(facts.facts()).containsEntry("lessPercent", 0).doesNotContainKey("loadAfterLess");
            assertThat(facts.text().key()).isEqualTo("assistant.template.plan.if_less.text.minimum");
            assertThat(facts.sources()).containsExactly(
                    CatalogText.of("assistant.template.source.sampling_undated", 5));
        }
    }

    @Nested
    @DisplayName("plan.if_none")
    class IfNone {

        private final IfNoneFactsAssembler assembler = new IfNoneFactsAssembler();

        @Test
        @DisplayName("Load kept without thinning against the target: 0.57 is 1.43 times 0.40")
        void keepsTheLoad() {
            var facts = assembler.assemble(plan(issuedPlan())).orElseThrow();

            assertThat(assembler.questionId()).isEqualTo(QuestionId.PLAN_IF_NONE);
            assertThat(facts.facts()).containsEntry("loadRatio", 1.43).doesNotContainKey("yieldEffect");
            assertThat(facts.text().key()).isEqualTo("assistant.template.plan.if_none.text");
            assertTwoToneCards(facts);
            assertThat(facts.actions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("plan.best_time")
    class BestTime {

        private final BestTimeFactsAssembler assembler = new BestTimeFactsAssembler();

        @Test
        @DisplayName("Open window: dates, days left including today and the register action")
        void openWindow() {
            var facts = assembler.assemble(plan(issuedPlan())).orElseThrow();

            assertThat(assembler.questionId()).isEqualTo(QuestionId.PLAN_BEST_TIME);
            assertThat(facts.facts()).containsEntry("windowState", "OPEN").containsEntry("daysLeft", 12L);
            assertThat(facts.text().key()).isEqualTo("assistant.template.plan.best_time.text.open");
            assertTwoToneCards(facts);
            assertThat(facts.actions()).extracting(ActionTemplate::target).containsExactly(DEEPLINK);
        }

        @Test
        @DisplayName("Before, closed and done windows have their own text and no action")
        void otherWindows() {
            var before = assembler.assemble(plan(plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20), null)))
                    .orElseThrow();
            var closed = assembler.assemble(plan(plan(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), null)))
                    .orElseThrow();
            var done = assembler.assemble(plan(plan(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10),
                    LocalDate.of(2026, 10, 28)))).orElseThrow();

            assertThat(before.facts()).containsEntry("windowState", "BEFORE").containsEntry("daysUntilOpen", 3L);
            assertThat(before.text().key()).isEqualTo("assistant.template.plan.best_time.text.before");
            assertThat(closed.text().key()).isEqualTo("assistant.template.plan.best_time.text.closed");
            assertThat(done.text().key()).isEqualTo("assistant.template.plan.best_time.text.done");
            assertThat(before.actions()).isEmpty();
            assertThat(closed.actions()).isEmpty();
            assertThat(done.actions()).isEmpty();
            assertTwoToneCards(before);
            assertTwoToneCards(closed);
            assertTwoToneCards(done);
        }
    }
}
