package com.arcadiadevs.viora.platform.assistant.application.internal.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantAnswerCommandService.AnswerOutcome;
import com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.WrittenText;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.infrastructure.ratelimit.SlidingWindowAssistantRateLimits;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture.PRODUCER;
import static com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture.T0;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
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
        service = serviceWith(fixture.rateLimits(1000, 1000));
    }

    private AssistantAnswerCommandServiceImpl serviceWith(SlidingWindowAssistantRateLimits rateLimits) {
        return new AssistantAnswerCommandServiceImpl(fixture.subscription, fixture.evaluator, fixture.renderer,
                fixture.writer, fixture.repository, rateLimits, fixture.metrics, fixture.clock, Duration.ofHours(6),
                Duration.ofMinutes(10));
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

    @Test
    @DisplayName("Stores the text, model and tokens the writer returns; cards, sources and actions stay the server's")
    void storesTheWrittenText() {
        var model = new AnswerModel("groq", "test-model", false);
        doReturn(new WrittenText("Tu carga está en 0,57 y el objetivo en 0,40.", model, 210, 38, null))
                .when(fixture.writer).write(any());

        var answer = ask(UUID.randomUUID(), false, ES).success().orElseThrow().view().answer();

        assertThat(answer.text()).isEqualTo("Tu carga está en 0,57 y el objetivo en 0,40.");
        assertThat(answer.model()).isEqualTo(model);
        assertThat(answer.inputTokens()).isEqualTo(210);
        assertThat(answer.outputTokens()).isEqualTo(38);
        assertThat(answer.facts()).hasSize(2);
        assertThat(answer.sources()).hasSize(1);
        assertThat(answer.actions()).hasSize(1);
        assertThat(fixture.repository.all().getFirst().inputTokens()).isEqualTo(210);
    }

    @Test
    @DisplayName("The writer receives the facts, the language, the template text and the localized actions")
    void passesTheDraftToTheWriter() {
        ask(UUID.randomUUID(), false, ES);

        var draft = org.mockito.ArgumentCaptor.forClass(AnswerDraft.class);
        verify(fixture.writer).write(draft.capture());
        assertThat(draft.getValue().questionId()).isEqualTo(QuestionId.PLAN_WHY_PERCENT);
        assertThat(draft.getValue().locale().getLanguage()).isEqualTo("es");
        assertThat(draft.getValue().facts()).containsEntry("percentToRemove", 30);
        assertThat(draft.getValue().templateText()).startsWith("Tu carga está en 0,57 frutos por brote");
        assertThat(draft.getValue().actions()).extracting("label").containsExactly("Registrar aclareo");
    }

    @Test
    @DisplayName("The writer (the model) is called once per new answer: never on a replay or a cache hit")
    void replayAndCacheNeverCallTheWriter() {
        var key = UUID.randomUUID();
        ask(key, false, ES);
        ask(key, false, ES);
        ask(UUID.randomUUID(), false, ES);

        verify(fixture.writer, times(1)).write(any());
    }

    @Test
    @DisplayName("Over the answers limit answers 429 with the seconds to wait; a replay never spends the limit")
    void limitsTheAnswersPerProducer() {
        service = serviceWith(fixture.rateLimits(2, 5));
        var key = UUID.randomUUID();
        ask(key, false, ES);
        ask(key, false, ES);
        ask(key, false, ES);
        ask(UUID.randomUUID(), false, ES);
        fixture.clock.advance(Duration.ofMinutes(15));

        var limited = ask(UUID.randomUUID(), false, ES);
        var replayed = ask(key, false, ES);

        assertThat(limited.failure().map(ApplicationError::code)).contains("ASSISTANT_TOO_MANY_REQUESTS");
        assertThat(limited.failure().map(ApplicationError::details)).contains("assistant.rate_limited");
        assertThat(limited.failure().map(error -> error.properties().get("retryAfterSeconds")))
                .contains(45L * 60);
        assertThat(replayed.success()).isPresent();
        assertThat(fixture.answers("plan.why_percent", "rate_limited")).isEqualTo(1);
        assertThat(fixture.answers("plan.why_percent", "replay")).isEqualTo(3);
    }

    @Test
    @DisplayName("forceRefresh has its own limit on top of the answers limit, and a refused one spends no answer")
    void limitsTheRefreshes() {
        service = serviceWith(fixture.rateLimits(4, 1));
        ask(UUID.randomUUID(), true, ES);

        var refused = ask(UUID.randomUUID(), true, ES);
        var plain = ask(UUID.randomUUID(), false, ES);
        var plainAgain = ask(UUID.randomUUID(), false, ES);
        var plainLast = ask(UUID.randomUUID(), false, ES);
        var overAnswers = ask(UUID.randomUUID(), false, ES);

        assertThat(refused.failure().map(ApplicationError::code)).contains("ASSISTANT_TOO_MANY_REQUESTS");
        assertThat(plain.success()).isPresent();
        assertThat(plainAgain.success()).isPresent();
        assertThat(plainLast.success()).isPresent();
        assertThat(overAnswers.failure().map(ApplicationError::code)).contains("ASSISTANT_TOO_MANY_REQUESTS");
    }

    @Test
    @DisplayName("Without an active plan the limit is not spent")
    void inactivePlanSpendsNoLimit() {
        service = serviceWith(fixture.rateLimits(1, 1));
        when(fixture.subscription.hasActivePlan(PRODUCER)).thenReturn(false);
        ask(UUID.randomUUID(), false, ES);
        when(fixture.subscription.hasActivePlan(PRODUCER)).thenReturn(true);

        assertThat(ask(UUID.randomUUID(), false, ES).success()).isPresent();
    }

    @Test
    @DisplayName("With a model writer, a template answer is served from cache only for the fallback TTL")
    void fallbackAnswersExpireSooner() {
        doReturn(true).when(fixture.writer).usesModel();
        var fallback = ask(UUID.randomUUID(), false, ES).success().orElseThrow();
        fixture.clock.advance(Duration.ofMinutes(10).minusSeconds(1));
        var cached = ask(UUID.randomUUID(), false, ES).success().orElseThrow();
        fixture.clock.advance(Duration.ofSeconds(1));

        var retried = ask(UUID.randomUUID(), false, ES).success().orElseThrow();

        assertThat(fallback.view().answer().model().fallback()).isTrue();
        assertThat(cached.created()).isFalse();
        assertThat(retried.created()).isTrue();
        verify(fixture.writer, times(2)).write(any());
    }

    @Test
    @DisplayName("A model answer keeps the normal TTL, and so do template answers when no model writes the texts")
    void modelAndTemplateAnswersKeepTheTtl() {
        doReturn(true).when(fixture.writer).usesModel();
        doReturn(new WrittenText("Tu carga está en 0,57 y el objetivo en 0,40.",
                new AnswerModel("groq", "test-model", false), 210, 38, null)).when(fixture.writer).write(any());
        ask(UUID.randomUUID(), false, ES);
        fixture.clock.advance(Duration.ofHours(1));
        var modelCached = ask(UUID.randomUUID(), false, ES).success().orElseThrow();
        var templateService = serviceWith(fixture.rateLimits(1000, 1000));
        doReturn(false).when(fixture.writer).usesModel();
        fixture.givenData(plan(plan(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 20), null)));
        doReturn(WrittenText.template("Plantilla.")).when(fixture.writer).write(any());
        templateService.handle(new AskQuestionCommand(PRODUCER, QuestionId.PLAN_WHY_PERCENT, PLOT, UUID.randomUUID(),
                false, ES));
        fixture.clock.advance(Duration.ofHours(1));

        var templateCached = templateService.handle(new AskQuestionCommand(PRODUCER, QuestionId.PLAN_WHY_PERCENT,
                PLOT, UUID.randomUUID(), false, ES)).success().orElseThrow();

        assertThat(modelCached.created()).isFalse();
        assertThat(templateCached.created()).isFalse();
    }

    @Test
    @DisplayName("Counts each outcome by question: created, cache, replay and unavailable")
    void countsTheOutcomes() {
        var key = UUID.randomUUID();
        ask(key, false, ES);
        ask(key, false, ES);
        ask(UUID.randomUUID(), false, ES);
        fixture.givenCatalogFacts(AssistantServiceFixture.withoutPlan());
        ask(UUID.randomUUID(), false, ES);

        assertThat(fixture.answers("plan.why_percent", "created")).isEqualTo(1);
        assertThat(fixture.answers("plan.why_percent", "replay")).isEqualTo(1);
        assertThat(fixture.answers("plan.why_percent", "cache")).isEqualTo(1);
        assertThat(fixture.answers("plan.why_percent", "unavailable")).isEqualTo(1);
    }

    @Test
    @DisplayName("Two requests with one key, one creating and one served from cache, end with a single answer")
    void racingRequestsShareOneAnswer() throws Exception {
        var cachedId = ask(UUID.randomUUID(), false, ES).success().orElseThrow().view().answer().id();
        var key = UUID.randomUUID();
        var writing = new CountDownLatch(1);
        var bound = new CountDownLatch(1);
        doAnswer(invocation -> {
            writing.countDown();
            assertThat(bound.await(5, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(fixture.writer).write(any());

        try (var pool = Executors.newSingleThreadExecutor()) {
            // The refresh skips the cache and stops inside the writer
            Future<Result<AnswerOutcome, ApplicationError>> creating = pool.submit(() -> ask(key, true, ES));
            assertThat(writing.await(5, TimeUnit.SECONDS)).isTrue();
            // Meanwhile the same key is served from the cache and bound to the cached answer
            var served = ask(key, false, ES).success().orElseThrow();
            bound.countDown();
            var created = creating.get(5, TimeUnit.SECONDS).success().orElseThrow();

            assertThat(served.view().answer().id()).isEqualTo(cachedId);
            assertThat(created.view().answer().id()).isEqualTo(cachedId);
            assertThat(created.created()).isFalse();
        }
        assertThat(fixture.repository.findByClientRequestId(PRODUCER, key).map(answer -> answer.id()))
                .contains(cachedId);
        assertThat(fixture.repository.all()).hasSize(1);
    }

    private Result<AnswerOutcome, ApplicationError> ask(UUID key, boolean forceRefresh, Locale locale) {
        return service.handle(new AskQuestionCommand(PRODUCER, QuestionId.PLAN_WHY_PERCENT, PLOT, key, forceRefresh,
                locale));
    }
}
