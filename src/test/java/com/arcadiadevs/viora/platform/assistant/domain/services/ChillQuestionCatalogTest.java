package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillDataSamples.chill;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillDataSamples.closedShort;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillDataSamples.completedInSeason;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillDataSamples.inProgress;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillDataSamples.insufficient;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillDataSamples.lastWinter;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillDataSamples.notEvaluated;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Question catalog: WINTER_CHILL availability")
class ChillQuestionCatalogTest {

    @Test
    @DisplayName("WINTER_CHILL is a plot screen whose questions use the contract prefix chill.")
    void screen() {
        assertThat(ScreenKey.fromValue("winter_chill")).isEqualTo(ScreenKey.WINTER_CHILL);
        assertThat(ScreenKey.WINTER_CHILL.requiresPlot()).isTrue();
        assertThat(ScreenKey.WINTER_CHILL.readsChill()).isTrue();
        assertThat(ScreenKey.WINTER_CHILL.readsClimate()).isFalse();
        assertThat(ScreenKey.WINTER_CHILL.questionPrefix()).isEqualTo("chill.");
        assertThat(ScreenKey.CLIMATE.questionPrefix()).isEqualTo("climate.");
        assertThat(QuestionId.CHILL_REACH_30.value()).isEqualTo("chill.reach_30");
        assertThat(QuestionCatalog.screenOf(QuestionId.CHILL_REACH_30)).contains(ScreenKey.WINTER_CHILL);
        assertThat(QuestionCatalog.screenOf(new QuestionId("chill.if_incomplete"))).contains(ScreenKey.WINTER_CHILL);
    }

    @Test
    @DisplayName("Winter in progress: four questions in contract order, all enabled, labels with the real threshold")
    void inProgressWinter() {
        var questions = chillScreen(chill(inProgress(), lastWinter()));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "chill.missing", "chill.reach_30", "chill.vs_last_winter", "chill.if_incomplete");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("temp", "calendar", "history",
                "alert");
        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.chill.missing.label"),
                CatalogText.of("assistant.question.chill.reach_30.label", 30.0),
                CatalogText.of("assistant.question.chill.vs_last_winter.label"),
                CatalogText.of("assistant.question.chill.if_incomplete.label", 30.0));
    }

    @Test
    @DisplayName("After August 31 the questions are about the finished winter and name its year")
    void finishedWinter() {
        var questions = chillScreen(chill(closedShort(), lastWinter()));

        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.chill.missing.label.closed", "2026"),
                CatalogText.of("assistant.question.chill.reach_30.label.closed", 30.0, "2026"),
                CatalogText.of("assistant.question.chill.vs_last_winter.label.closed", "2026", "2025"),
                CatalogText.of("assistant.question.chill.if_incomplete.label.closed", 30.0, "2026"));
    }

    @Test
    @DisplayName("Without chill data every question is disabled: \"Sin datos de frío para este lote\"")
    void withoutData() {
        for (var data : List.of(ChillData.NONE, chill(notEvaluated(), lastWinter()))) {
            var questions = chillScreen(data);
            assertThat(questions).noneMatch(CatalogQuestion::enabled);
            assertThat(questions).extracting(CatalogQuestion::disabledReason)
                    .containsOnly(CatalogText.of("assistant.reason.chill_missing"));
        }
    }

    @Test
    @DisplayName("Too few days to project disables only chill.reach_30; without last winter only the comparison")
    void partialData() {
        var early = chillScreen(chill(insufficient(), lastWinter()));
        assertThat(early).extracting(CatalogQuestion::enabled).containsExactly(true, false, true, true);
        assertThat(early.get(1).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.chill_pace_missing"));

        var noLastWinter = chillScreen(chill(inProgress(), null));
        assertThat(noLastWinter).extracting(CatalogQuestion::enabled).containsExactly(true, true, false, true);
        assertThat(noLastWinter.get(2).disabledReason())
                .isEqualTo(CatalogText.of("assistant.reason.last_winter_missing"));

        // A completed winter needs no projection
        assertThat(chillScreen(chill(completedInSeason(), null)).get(1).enabled()).isTrue();
    }

    private static List<CatalogQuestion> chillScreen(ChillData chill) {
        return QuestionCatalog.questionsFor(ScreenKey.WINTER_CHILL, PlotFacts.ofChill(2026, chill));
    }
}
