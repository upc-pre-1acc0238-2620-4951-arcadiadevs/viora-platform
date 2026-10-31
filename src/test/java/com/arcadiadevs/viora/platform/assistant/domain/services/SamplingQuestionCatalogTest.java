package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.bare;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.halfway;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.withPlan;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question catalog: SAMPLING_SUMMARY availability")
class SamplingQuestionCatalogTest {

    @Test
    @DisplayName("SAMPLING_SUMMARY is a one-plot screen with the prefix sampling.")
    void screen() {
        assertThat(ScreenKey.fromValue("sampling_summary")).isEqualTo(ScreenKey.SAMPLING_SUMMARY);
        assertThat(ScreenKey.SAMPLING_SUMMARY.requiresPlot()).isTrue();
        assertThat(ScreenKey.SAMPLING_SUMMARY.readsOnePlot()).isTrue();
        assertThat(ScreenKey.PLOT_DETAIL.readsOnePlot()).isTrue();
        assertThat(ScreenKey.HOME.readsOnePlot()).isFalse();
        assertThat(ScreenKey.SAMPLING_SUMMARY.readsClimate()).isFalse();
        assertThat(ScreenKey.SAMPLING_SUMMARY.questionPrefix()).isEqualTo("sampling.");
        assertThat(QuestionCatalog.screenOf(QuestionId.SAMPLING_CORRECT_TREE)).contains(ScreenKey.SAMPLING_SUMMARY);
    }

    @Test
    @DisplayName("Issued prescription: contract order, all enabled but correct_tree (Thinning cannot correct a tree)")
    void issued() {
        var questions = sampling(issuedPlan());

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "sampling.enough_trees", "sampling.meaning", "sampling.next", "sampling.correct_tree");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("lote", "fruit", "calendar",
                "pruning");
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, true, true, false);
        assertThat(questions.get(3).disabledReason())
                .isEqualTo(CatalogText.of("assistant.reason.tree_correction_unavailable"));
    }

    @Test
    @DisplayName("Sampling halfway: meaning waits for the plan; never sampled: only correct_tree's reason differs")
    void halfwayAndNever() {
        var half = sampling(halfway());
        assertThat(half).extracting(CatalogQuestion::enabled).containsExactly(true, false, true, false);
        assertThat(half.get(1).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.plan_missing"));

        var never = sampling(null);
        assertThat(never).noneMatch(CatalogQuestion::enabled);
        assertThat(never.subList(0, 3)).extracting(CatalogQuestion::disabledReason)
                .containsOnly(CatalogText.of("assistant.reason.sampling_missing"));
    }

    private static List<CatalogQuestion> sampling(ThinningPlanData plan) {
        return QuestionCatalog.questionsFor(ScreenKey.SAMPLING_SUMMARY, PlotFacts.ofOnePlot(2026, null,
                new HomeData(false, List.of(plan == null ? bare() : withPlan(plan, 0)), null)));
    }
}
