package com.arcadiadevs.viora.platform.assistant.domain.model;

import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetLatestAnswerQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Field scope: only HOME may leave the plot out")
class FieldScopeTest {

    private static final String PRODUCER = UUID.randomUUID().toString();

    @Test
    @DisplayName("HOME is the only screen without a required plot")
    void onlyHomeHasFieldScope() {
        assertThat(ScreenKey.fromValue("home")).isEqualTo(ScreenKey.HOME);
        assertThat(Arrays.stream(ScreenKey.values()).filter(screen -> !screen.requiresPlot()))
                .containsExactly(ScreenKey.HOME);
        assertThat(ScreenKey.HOME.readsClimate()).isFalse();
        assertThat(ScreenKey.HOME.questionPrefix()).isEqualTo("home.");
    }

    @Test
    @DisplayName("A HOME question, catalog or latest answer without plot is accepted; any other is not")
    void plotRequiredOutsideHome() {
        assertThat(new AskQuestionCommand(PRODUCER, QuestionId.HOME_WEEK_PLAN, null, UUID.randomUUID(), false,
                Locale.ENGLISH).plotId()).isNull();
        assertThat(new GetLatestAnswerQuery(PRODUCER, ScreenKey.HOME, null).plotId()).isNull();
        assertThat(new GetQuestionCatalogQuery(PRODUCER, ScreenKey.HOME, null).plotId()).isNull();

        for (var questionId : List.of(QuestionId.ALTERNATION_WHY_INDEX, QuestionId.PLAN_IF_LESS,
                QuestionId.ALERT_WHEN_ENDS, QuestionId.CLIMATE_IRRIGATE, new QuestionId("unknown.question"))) {
            assertThatThrownBy(() -> new AskQuestionCommand(PRODUCER, questionId, null, UUID.randomUUID(), false,
                    Locale.ENGLISH)).hasMessage("assistant.plot.required");
        }
        for (var screen : List.of(ScreenKey.ALTERNATION, ScreenKey.PLAN, ScreenKey.ALERT, ScreenKey.CLIMATE)) {
            assertThatThrownBy(() -> new GetLatestAnswerQuery(PRODUCER, screen, null))
                    .hasMessage("assistant.plot.required");
        }
    }

    @Test
    @DisplayName("Answer data without plot exists only for a HOME question about the whole field")
    void answerDataWithoutPlot() {
        var today = LocalDate.of(2026, 10, 30);
        assertThat(PlotAnswerData.ofHome(null, today, 2026, new HomeData(true, List.of(), null)).plotId()).isNull();
        assertThatThrownBy(() -> PlotAnswerData.ofHome(null, today, 2026, new HomeData(false, List.of(), null)))
                .hasMessage("assistant.answer_data.invalid");
        assertThatThrownBy(() -> new PlotAnswerData(null, today, 2026, null, null, null, null))
                .hasMessage("assistant.answer_data.invalid");
    }
}
