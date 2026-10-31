package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData.PlotHome;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.thinnedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.bare;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.halfway;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.withPlan;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question catalog: PLOT_DETAIL availability")
class PlotDetailQuestionCatalogTest {

    @Test
    @DisplayName("PLOT_DETAIL is a plot screen whose questions use the contract prefix plot.")
    void screen() {
        assertThat(ScreenKey.fromValue("plot_detail")).isEqualTo(ScreenKey.PLOT_DETAIL);
        assertThat(ScreenKey.PLOT_DETAIL.requiresPlot()).isTrue();
        assertThat(ScreenKey.PLOT_DETAIL.readsClimate()).isFalse();
        assertThat(ScreenKey.PLOT_DETAIL.readsChill()).isFalse();
        assertThat(ScreenKey.PLOT_DETAIL.questionPrefix()).isEqualTo("plot.");
        assertThat(ScreenKey.WINTER_CHILL.questionPrefix()).isEqualTo("chill.");
        assertThat(ScreenKey.PLAN.questionPrefix()).isEqualTo("plan.");
        assertThat(QuestionCatalog.screenOf(QuestionId.PLOT_NEXT_YEAR_HARVEST)).contains(ScreenKey.PLOT_DETAIL);
        assertThat(QuestionCatalog.screenOf(new QuestionId("plot.how_is"))).contains(ScreenKey.PLOT_DETAIL);
    }

    @Test
    @DisplayName("Thinned plot with index and caliber: four questions in contract order, all enabled")
    void everythingKnown() {
        var questions = plotDetail(0.51, thinnedPlan(14250.0));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "plot.how_is", "plot.today", "plot.last_sampling", "plot.next_year_harvest");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("lote", "calendar", "fruit",
                "harvest");
        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.plot.how_is.label"),
                CatalogText.of("assistant.question.plot.today.label"),
                CatalogText.of("assistant.question.plot.last_sampling.label"),
                CatalogText.of("assistant.question.plot.next_year_harvest.label", "2027"));
    }

    @Test
    @DisplayName("next_year_harvest is disabled without caliber (contract): \"Faltan datos de calibre\"")
    void withoutCaliber() {
        var questions = plotDetail(0.51, issuedPlan());

        assertThat(questions.subList(0, 3)).allMatch(CatalogQuestion::enabled);
        assertThat(questions.get(3).enabled()).isFalse();
        assertThat(questions.get(3).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.caliber_missing"));
    }

    @Test
    @DisplayName("Never sampled: last_sampling disabled; with neither index nor sampling how_is too; today stays")
    void neverSampled() {
        var withIndex = plotDetail(0.51, null);
        assertThat(withIndex.get(0).enabled()).isTrue();
        assertThat(withIndex.get(2).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.sampling_missing"));

        var nothing = plotDetail(null, null);
        assertThat(nothing).extracting(CatalogQuestion::enabled).containsExactly(false, true, false, false);
        assertThat(nothing.get(0).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.plot_data_missing"));

        var sampledWithoutIndex = QuestionCatalog.questionsFor(ScreenKey.PLOT_DETAIL, PlotFacts.ofOnePlot(2026,
                null, new HomeData(false, List.of(withPlan(halfway(), 2)), null)));
        assertThat(sampledWithoutIndex).extracting(CatalogQuestion::enabled)
                .containsExactly(true, true, true, false);
    }

    private static List<CatalogQuestion> plotDetail(Double bearingIndex, ThinningPlanData plan) {
        PlotHome plot = plan == null ? bare() : withPlan(plan, 0);
        return QuestionCatalog.questionsFor(ScreenKey.PLOT_DETAIL,
                PlotFacts.ofOnePlot(2026, bearingIndex, new HomeData(false, List.of(plot), null)));
    }
}
