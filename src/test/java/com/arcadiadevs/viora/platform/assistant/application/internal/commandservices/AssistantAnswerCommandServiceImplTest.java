package com.arcadiadevs.viora.platform.assistant.application.internal.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantAnswerCommandService.AnswerOutcome;
import com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture.PRODUCER;
import static com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture.T0;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("AssistantAnswerCommandService: templates, cache and idempotency")
class AssistantAnswerCommandServiceImplTest {

    private static final Locale ES = Locale.forLanguageTag("es");

    private AssistantServiceFixture fixture;
    private AssistantAnswerCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        fixture = new AssistantServiceFixture();
        service = new AssistantAnswerCommandServiceImpl(fixture.subscription, fixture.evaluator, fixture.renderer,
                fixture.repository, fixture.clock, Duration.ofHours(6));
    }

    @Test
    @DisplayName("Creates a template answer: text, two cards, sources, action, template model, version")
    void createsAnAnswer() {
        var outcome = ask(UUID.randomUUID(), false, ES).success().orElseThrow();

        var answer = outcome.view().answer();
        assertThat(outcome.created()).isTrue();
        assertThat(outcome.view().freshness()).isEqualTo(Freshness.UP_TO_DATE);
        assertThat(answer.producerId()).isEqualTo(PRODUCER);
        assertThat(answer.questionId()).isEqualTo(QuestionId.PLAN_WHY_PERCENT);
        assertThat(answer.locale()).isEqualTo("es");
        assertThat(answer.text()).startsWith("Tu carga está en 0,57 frutos por brote");
        assertThat(answer.facts()).hasSize(2);
        assertThat(answer.sources()).hasSize(1);
        assertThat(answer.actions()).hasSize(1);
        assertThat(answer.model()).isEqualTo(AnswerModel.TEMPLATE);
        assertThat(answer.dataVersion().value()).startsWith("sha256:");
        assertThat(answer.createdAt()).isEqualTo(T0);
        assertThat(fixture.repository.all()).hasSize(1);
    }

    @Test
    @DisplayName("The same clientRequestId returns the same answer without building the facts again")
    void replaysTheSameRequest() {
        var key = UUID.randomUUID();
        var first = ask(key, false, ES).success().orElseThrow();
        fixture.clock.advance(Duration.ofHours(7));

        var second = ask(key, true, ES).success().orElseThrow();

        assertThat(second.created()).isFalse();
        assertThat(second.view().answer().id()).isEqualTo(first.view().answer().id());
        assertThat(second.view().freshness()).isEqualTo(Freshness.UP_TO_DATE);
        verify(fixture.whyPercent, times(1)).assemble(any());
        assertThat(fixture.repository.all()).hasSize(1);
    }

    @Test
    @DisplayName("A clientRequestId used for another question is a conflict")
    void rejectsAReusedKey() {
        var key = UUID.randomUUID();
        ask(key, false, ES);

        var result = service.handle(new AskQuestionCommand(PRODUCER, QuestionId.PLAN_IF_LESS, PLOT, key, false, ES));

        assertThat(result.failure().map(ApplicationError::code)).contains("ASSISTANT_REQUEST_CONFLICT");
        assertThat(result.failure().map(ApplicationError::details)).contains("assistant.request.reused");
    }

    @Test
    @DisplayName("A key served from the cache keeps that answer on retry, even after the TTL, and cannot be reused")
    void cacheHitBindsItsKey() {
        var first = ask(UUID.randomUUID(), false, ES).success().orElseThrow();
        var key = UUID.randomUUID();
        var cached = ask(key, false, ES).success().orElseThrow();
        fixture.clock.advance(Duration.ofHours(7));

        var retried = ask(key, false, ES).success().orElseThrow();
        var reused = service.handle(new AskQuestionCommand(PRODUCER, QuestionId.PLAN_IF_LESS, PLOT, key, false, ES));

        assertThat(cached.view().answer().id()).isEqualTo(first.view().answer().id());
        assertThat(retried.created()).isFalse();
        assertThat(retried.view().answer().id()).isEqualTo(first.view().answer().id());
        assertThat(reused.failure().map(ApplicationError::details)).contains("assistant.request.reused");
        assertThat(fixture.repository.all()).hasSize(1);
    }

    @Test
    @DisplayName("Same facts and language within the TTL serve the stored answer; from the TTL on a new one")
    void servesFromCacheWithinTheTtl() {
        var first = ask(UUID.randomUUID(), false, ES).success().orElseThrow();

        fixture.clock.advance(Duration.ofHours(6).minusSeconds(1));
        var cached = ask(UUID.randomUUID(), false, ES).success().orElseThrow();
        fixture.clock.advance(Duration.ofSeconds(1));
        var renewed = ask(UUID.randomUUID(), false, ES).success().orElseThrow();

        assertThat(cached.created()).isFalse();
        assertThat(cached.view().answer().id()).isEqualTo(first.view().answer().id());
        assertThat(renewed.created()).isTrue();
        assertThat(renewed.view().answer().id()).isNotEqualTo(first.view().answer().id());
        assertThat(fixture.repository.all()).hasSize(2);
    }

    @Test
    @DisplayName("Another language, or new facts, are not served from cache")
    void cacheKeyIncludesLanguageAndFacts() {
        var spanish = ask(UUID.randomUUID(), false, ES).success().orElseThrow();
        var english = ask(UUID.randomUUID(), false, Locale.ENGLISH).success().orElseThrow();
        fixture.givenData(plan(plan(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 20), null)));
        var newFacts = ask(UUID.randomUUID(), false, ES).success().orElseThrow();

        assertThat(english.created()).isTrue();
        assertThat(english.view().answer().locale()).isEqualTo("en");
        assertThat(english.view().answer().text()).startsWith("Your load is 0.57");
        assertThat(newFacts.created()).isTrue();
        assertThat(newFacts.view().answer().dataVersion()).isNotEqualTo(spanish.view().answer().dataVersion());
    }

    @Test
    @DisplayName("forceRefresh skips the cache but still honours the clientRequestId")
    void forceRefreshSkipsTheCache() {
        var first = ask(UUID.randomUUID(), false, ES).success().orElseThrow();
        var key = UUID.randomUUID();

        var refreshed = ask(key, true, ES).success().orElseThrow();
        var replayed = ask(key, true, ES).success().orElseThrow();

        assertThat(refreshed.created()).isTrue();
        assertThat(refreshed.view().answer().id()).isNotEqualTo(first.view().answer().id());
        assertThat(replayed.created()).isFalse();
        assertThat(replayed.view().answer().id()).isEqualTo(refreshed.view().answer().id());
        assertThat(fixture.repository.all()).hasSize(2);
    }

    @Test
    @DisplayName("A disabled question answers 409 with its reason, and nothing is read or assembled")
    void disabledQuestionCallsNothing() {
        fixture.givenCatalogFacts(AssistantServiceFixture.withoutPlan());

        var result = ask(UUID.randomUUID(), false, ES);

        assertThat(result.failure().map(ApplicationError::code)).contains("ASSISTANT_QUESTION_CONFLICT");
        assertThat(result.failure().map(ApplicationError::details)).contains("Sin plan de aclareo activo");
        assertThat(result.failure().map(error -> error.properties().get("reason")))
                .contains("assistant.question.unavailable");
        verify(fixture.whyPercent, never()).assemble(any());
        verifyNoInteractions(fixture.dataReader);
        assertThat(fixture.repository.all()).isEmpty();
    }

    @Test
    @DisplayName("Without an active plan answers 403 and asks nothing else")
    void requiresAnActivePlan() {
        when(fixture.subscription.hasActivePlan(PRODUCER)).thenReturn(false);

        var result = ask(UUID.randomUUID(), false, ES);

        assertThat(result.failure().map(ApplicationError::code)).contains("ASSISTANT_FORBIDDEN");
        assertThat(result.failure().map(ApplicationError::details)).contains("assistant.subscription.inactive");
        verifyNoInteractions(fixture.catalog, fixture.dataReader);
    }

    private Result<AnswerOutcome, ApplicationError> ask(UUID key, boolean forceRefresh, Locale locale) {
        return service.handle(new AskQuestionCommand(PRODUCER, QuestionId.PLAN_WHY_PERCENT, PLOT, key, forceRefresh,
                locale));
    }
}
