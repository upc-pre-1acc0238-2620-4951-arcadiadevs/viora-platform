package com.arcadiadevs.viora.platform.assistant.domain.model;

import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetLatestAnswerQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClientRequestId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Field scope: only HOME may go without a plot")
class FieldScopeTest {

    private static final String PRODUCER = UUID.randomUUID().toString();

    @Test
    @DisplayName("HOME is the only screen without a required plot")
    void onlyHomeHasFieldScope() {
        assertThat(ScreenKey.HOME.requiresPlot()).isFalse();
        assertThat(Arrays.stream(ScreenKey.values()).filter(screen -> screen != ScreenKey.HOME))
                .isNotEmpty().allSatisfy(screen -> assertThat(screen.requiresPlot()).as(screen.name()).isTrue());
    }

    @Test
    @DisplayName("asking without plot is a HOME question; any other question still needs its plot")
    void askWithoutPlot() {
        var command = new AskQuestionCommand(PRODUCER, QuestionId.HOME_WEEK_PLAN, null, key(), false,
                AnswerLanguage.SPANISH);
        assertThat(command.plotId()).isNull();

        for (var question : List.of(QuestionId.PLAN_WHY_PERCENT, QuestionId.ALERT_WHAT_TO_DO,
                QuestionId.PLOT_TODAY, QuestionId.CAMPAIGN_PLAN_NEXT)) {
            assertThatThrownBy(() -> new AskQuestionCommand(PRODUCER, question, null, key(), false,
                    AnswerLanguage.SPANISH)).as(question.value())
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.plot.required");
        }
    }

    @Test
    @DisplayName("the latest answer and the catalog of HOME without plot are the field's; other screens need it")
    void queriesWithoutPlot() {
        assertThat(new GetLatestAnswerQuery(PRODUCER, ScreenKey.HOME, null).plotId()).isNull();
        assertThat(new GetQuestionCatalogQuery(PRODUCER, ScreenKey.HOME, null).plotId()).isNull();
        for (var screen : ScreenKey.values()) {
            if (screen == ScreenKey.HOME) {
                continue;
            }
            assertThatThrownBy(() -> new GetLatestAnswerQuery(PRODUCER, screen, null)).as(screen.name())
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.plot.required");
            assertThatThrownBy(() -> new GetQuestionCatalogQuery(PRODUCER, screen, null)).as(screen.name())
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.plot.required");
        }
    }

    @Test
    @DisplayName("an answer without plot can only be of a HOME question")
    void answerWithoutPlot() {
        assertThat(answer(QuestionId.HOME_ALERTS_AFFECTING).plotId()).isNull();
        assertThatThrownBy(() -> answer(QuestionId.PLAN_WHY_PERCENT))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.answer.invalid");
    }

    private static AssistantAnswer answer(QuestionId question) {
        return new AssistantAnswer(AnswerId.generate(), PRODUCER, null, question, "¿Qué alertas afectan a mis lotes?",
                AnswerLanguage.SPANISH, new DataVersion("sha256:abc"), "Respuesta.",
                List.of(new AnswerFact("1", "a", "b", FactTone.PRIMARY),
                        new AnswerFact("2", "c", "d", FactTone.HIGHLIGHT)),
                List.of(), List.of(), key(), AssistantAnswer.TEMPLATE, null, false, null, null, Instant.now());
    }

    private static ClientRequestId key() {
        return new ClientRequestId(UUID.randomUUID().toString());
    }
}
