package com.arcadiadevs.viora.platform.assistant.domain.model.aggregates;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AssistantAnswer")
class AssistantAnswerTest {

    private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");
    private static final DataVersion VERSION = DataVersion.of(Map.of("percent", 30));
    private static final List<AnswerFact> FACTS = List.of(
            new AnswerFact("0,57", "frutos por brote", "objetivo 0,40", FactTone.PRIMARY),
            new AnswerFact("−30 %", "de los frutos", "ventana abierta", FactTone.HIGHLIGHT));

    @Test
    @DisplayName("Is created with a new id, the template model and no tokens")
    void createsAnAnswer() {
        var answer = answerAt(NOW);

        assertThat(answer.id()).isNotNull();
        assertThat(answer.producerId()).isEqualTo("producer-1");
        assertThat(answer.questionId()).isEqualTo(QuestionId.PLAN_WHY_PERCENT);
        assertThat(answer.locale()).isEqualTo("es");
        assertThat(answer.dataVersion()).isEqualTo(VERSION);
        assertThat(answer.facts()).isEqualTo(FACTS);
        assertThat(answer.model()).isEqualTo(AnswerModel.TEMPLATE);
        assertThat(answer.inputTokens()).isNull();
        assertThat(answer.outputTokens()).isNull();
        assertThat(answer.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Requires exactly two facts")
    void requiresTwoFacts() {
        assertThatThrownBy(() -> AssistantAnswer.create(UUID.randomUUID(), "producer-1",
                new PlotId(UUID.randomUUID().toString()), QuestionId.PLAN_WHY_PERCENT, "es", VERSION, "Texto.",
                FACTS.subList(0, 1), List.of(), List.of(), AnswerModel.TEMPLATE, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("assistant.answer.facts");
    }

    @Test
    @DisplayName("Is up to date while the current version matches, stale when it changed or is unknown")
    void freshness() {
        var answer = answerAt(NOW);

        assertThat(answer.freshnessAgainst(Optional.of(VERSION))).isEqualTo(Freshness.UP_TO_DATE);
        assertThat(answer.freshnessAgainst(Optional.of(DataVersion.of(Map.of("percent", 20)))))
                .isEqualTo(Freshness.STALE);
        assertThat(answer.freshnessAgainst(Optional.empty())).isEqualTo(Freshness.STALE);
    }

    @Test
    @DisplayName("Is kept 10 days and cached while younger than the TTL")
    void retentionAndCache() {
        var answer = answerAt(NOW);

        assertThat(answer.isRetainedAt(NOW.plus(Duration.ofDays(10)))).isTrue();
        assertThat(answer.isRetainedAt(NOW.plus(Duration.ofDays(10)).plusSeconds(1))).isFalse();
        assertThat(answer.isCachedAt(NOW.plus(Duration.ofHours(6)).minusSeconds(1), Duration.ofHours(6))).isTrue();
        assertThat(answer.isCachedAt(NOW.plus(Duration.ofHours(6)), Duration.ofHours(6))).isFalse();
    }

    private static AssistantAnswer answerAt(Instant createdAt) {
        return AssistantAnswer.create(UUID.randomUUID(), "producer-1", new PlotId(UUID.randomUUID().toString()),
                QuestionId.PLAN_WHY_PERCENT, "es", VERSION, "Texto.", FACTS, List.of(), List.of(),
                AnswerModel.TEMPLATE, createdAt);
    }
}
