package com.arcadiadevs.viora.platform.assistant.application.internal.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.application.internal.queryservices.AnswerFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.application.queryservices.QuestionCatalogQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClientRequestId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerComposer;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AskQuestionCommandService Unit Tests")
class AskQuestionCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-30T20:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration TTL = Duration.ofHours(6);

    @Mock
    private QuestionCatalogQueryService catalogQueryService;
    @Mock
    private AnswerFactsAssembler factsAssembler;
    @Mock
    private AssistantAnswerRepository repository;

    private AskQuestionCommandServiceImpl service;

    private final String producerId = UUID.randomUUID().toString();
    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());
    private final QuestionId question = QuestionId.PLAN_IF_NONE;
    private final ClientRequestId requestId = new ClientRequestId(UUID.randomUUID().toString());

    @BeforeEach
    void setUp() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        service = new AskQuestionCommandServiceImpl(catalogQueryService, factsAssembler, repository,
                new AnswerLocalizer(messages), CLOCK, TTL);
    }

    private AskQuestionCommand command(boolean forceRefresh) {
        return new AskQuestionCommand(producerId, question, plotId, requestId, forceRefresh, AnswerLanguage.SPANISH);
    }

    private void givenCatalog(boolean enabled) {
        var offered = new CatalogQuestion(question, CatalogText.of("assistant.question.plan.if_none.label"), "alert",
                enabled, enabled ? null : CatalogText.of("assistant.reason.plan_missing"));
        when(catalogQueryService.handle(any())).thenReturn(Result.success(
                new ScreenQuestionCatalog(ScreenKey.PLAN, plotId, List.of(offered))));
    }

    private static AnswerFacts prescription() {
        return new AnswerFacts(2026, null, new TreeMap<>(), null, null, 0.60, 0.40, 30.0, null, null, 5);
    }

    private AssistantAnswer savedAnswer() {
        return new AssistantAnswer(AnswerId.generate(), producerId, plotId, question, AnswerLanguage.SPANISH,
                new DataVersion("sha256:abc"), "Respuesta guardada",
                List.of(new AnswerFact("1", "a", "b", FactTone.PRIMARY), new AnswerFact("2", "c", "d", FactTone.HIGHLIGHT)),
                List.of(), List.of(), requestId, AssistantAnswer.TEMPLATE, null, false, null, null, NOW.minusSeconds(60));
    }

    @Test
    @DisplayName("an error of the catalog (no plan, foreign or unknown plot) is returned untouched, before anything is read")
    void propagatesTheCatalogError() {
        var error = ApplicationError.forbidden("assistant", "assistant.subscription.inactive");
        when(catalogQueryService.handle(any())).thenReturn(Result.failure(error));

        var result = service.handle(command(false));

        assertThat(result).isEqualTo(Result.failure(error));
        verifyNoInteractions(factsAssembler, repository);
    }

    @Test
    @DisplayName("a question the catalog does not list for the screen answers 400")
    void rejectsAQuestionOutsideTheCatalog() {
        when(catalogQueryService.handle(any())).thenReturn(Result.success(
                new ScreenQuestionCatalog(ScreenKey.PLAN, plotId, List.of())));

        var result = service.handle(command(false));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, ApplicationError>) result).error().code()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("a replay of the same client request id returns the saved answer even if the question has no data now")
    void replayReturnsTheSavedAnswer() {
        givenCatalog(false);
        var saved = savedAnswer();
        when(repository.findByClientRequestId(producerId, requestId)).thenReturn(Optional.of(saved));

        var result = service.handle(command(false));

        assertThat(((Result.Success<?, ?>) result).value()).hasFieldOrPropertyWithValue("answer", saved);
        verify(repository, never()).save(any());
        verifyNoInteractions(factsAssembler);
    }

    @Test
    @DisplayName("a disabled question answers 409 and nothing is saved")
    void rejectsADisabledQuestion() {
        givenCatalog(false);
        when(repository.findByClientRequestId(anyString(), any())).thenReturn(Optional.empty());

        var result = service.handle(command(false));

        assertThat(((Result.Failure<?, ApplicationError>) result).error().code()).isEqualTo("ASSISTANT_CONFLICT");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("an enabled question whose figures cannot be composed answers 409 instead of inventing them")
    void rejectsWhenTheFiguresAreMissing() {
        givenCatalog(true);
        when(repository.findByClientRequestId(anyString(), any())).thenReturn(Optional.empty());
        when(factsAssembler.assemble(ScreenKey.PLAN, plotId))
                .thenReturn(new AnswerFacts(2026, null, new TreeMap<>(), null, null, null, null, null, null, null, 0));
        when(repository.findReusable(anyString(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());

        var result = service.handle(command(false));

        assertThat(((Result.Failure<?, ApplicationError>) result).error().code()).isEqualTo("ASSISTANT_CONFLICT");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("a saved answer for the same data inside the cache window is reused without writing a new one")
    void reusesTheSavedAnswer() {
        givenCatalog(true);
        when(repository.findByClientRequestId(anyString(), any())).thenReturn(Optional.empty());
        when(factsAssembler.assemble(ScreenKey.PLAN, plotId)).thenReturn(prescription());
        var saved = savedAnswer();
        when(repository.findReusable(anyString(), any(), any(), any(), any(), any())).thenReturn(Optional.of(saved));

        var result = service.handle(command(false));

        assertThat(((Result.Success<?, ?>) result).value()).hasFieldOrPropertyWithValue("answer", saved);
        verify(repository).findReusable(producerId, plotId, question, AnswerLanguage.SPANISH,
                AnswerComposer.dataVersionOf(ScreenKey.PLAN, prescription()), NOW.minus(TTL));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("forceRefresh does not look for a reusable answer and saves a new one")
    void forceRefreshWritesANewAnswer() {
        givenCatalog(true);
        when(repository.findByClientRequestId(anyString(), any())).thenReturn(Optional.empty());
        when(factsAssembler.assemble(ScreenKey.PLAN, plotId)).thenReturn(prescription());

        var result = service.handle(command(true));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(repository, never()).findReusable(any(), any(), any(), any(), any(), any());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("two simultaneous asks with the same key: the loser reads the answer the winner saved")
    void lostRaceReadsTheWinnersAnswer() {
        givenCatalog(true);
        var saved = savedAnswer();
        when(repository.findByClientRequestId(anyString(), any()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(saved));
        when(factsAssembler.assemble(ScreenKey.PLAN, plotId)).thenReturn(prescription());
        when(repository.findReusable(anyString(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("uq_assistant_answer_request"))
                .when(repository).save(any());

        var result = service.handle(command(false));

        var view = ((Result.Success<AnswerView, ApplicationError>) result).value();
        assertThat(view.answer()).isEqualTo(saved);
        assertThat(view.freshness()).isEqualTo(Freshness.UP_TO_DATE);
    }
}
