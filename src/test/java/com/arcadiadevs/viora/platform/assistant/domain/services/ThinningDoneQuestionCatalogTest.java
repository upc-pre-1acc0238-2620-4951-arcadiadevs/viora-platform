package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.bare;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question catalog: THINNING_DONE availability")
class ThinningDoneQuestionCatalogTest {

    @Test
    @DisplayName("THINNING_DONE is a one-plot screen with the prefix thinning.")
    void screen() {
        assertThat(ScreenKey.fromValue("thinning_done")).isEqualTo(ScreenKey.THINNING_DONE);
        assertThat(ScreenKey.THINNING_DONE.requiresPlot()).isTrue();
        assertThat(ScreenKey.THINNING_DONE.readsOnePlot()).isTrue();
        assertThat(ScreenKey.THINNING_DONE.questionPrefix()).isEqualTo("thinning.");
        assertThat(QuestionCatalog.screenOf(QuestionId.THINNING_NEXT_SAMPLING)).contains(ScreenKey.THINNING_DONE);
    }

    @Test
    @DisplayName("Thinning recorded with yield and target: contract order, all enabled, the target in the label")
    void recorded() {
        var questions = thinningDone(new ThinningOutcomeData(LocalDate.of(2026, 10, 28), 30.0, "OPTIMAL", 0.57, 0.4,
                0.4, "BALANCED", 9100.0, 8200.0, "CALIBER_ADJUSTED", 3, "PROVISIONAL", 14, 49));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "thinning.harvest_change", "thinning.why_target", "thinning.next_sampling");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("harvest", "prio", "calendar");
        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions.get(1).label())
                .isEqualTo(CatalogText.of("assistant.question.thinning.why_target.label", 0.4));
    }

    @Test
    @DisplayName("Without yield or target only those wait; before the thinning every question is disabled")
    void missingData() {
        var partial = thinningDone(new ThinningOutcomeData(LocalDate.of(2026, 10, 28), null, null, null, null, null,
                null, null, null, null, 0, null, null, null));
        assertThat(partial).extracting(CatalogQuestion::enabled).containsExactly(false, false, true);
        assertThat(partial.get(0).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.yield_missing"));
        assertThat(partial.get(1).label())
                .isEqualTo(CatalogText.of("assistant.question.thinning.why_target.label_without_target"));

        var before = thinningDone(null);
        assertThat(before).noneMatch(CatalogQuestion::enabled);
        assertThat(before).extracting(CatalogQuestion::disabledReason)
                .containsOnly(CatalogText.of("assistant.reason.thinning_missing"));
    }

    private static List<CatalogQuestion> thinningDone(ThinningOutcomeData outcome) {
        return QuestionCatalog.questionsFor(ScreenKey.THINNING_DONE, PlotFacts.ofOnePlot(2026, null,
                new HomeData(false, List.of(bare()), null).withOutcome(outcome)));
    }
}
