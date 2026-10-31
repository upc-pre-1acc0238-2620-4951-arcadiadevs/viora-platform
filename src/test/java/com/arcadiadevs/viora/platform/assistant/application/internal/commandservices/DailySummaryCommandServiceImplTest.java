package com.arcadiadevs.viora.platform.assistant.application.internal.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerTextChooser;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ai.AnswerTextWriter;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ratelimit.AnswerRateLimiter;
import com.arcadiadevs.viora.platform.assistant.application.internal.queryservices.AnswerFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.DailySummary;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DailySummaryId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.DailySummaryRepository;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerComposer;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailySummaryCommandService")
class DailySummaryCommandServiceImplTest {

    /** 15:00 in Lima on 30 October 2026. */
    private static final Instant NOW = Instant.parse("2026-10-30T20:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);
    /** Midnight of 30 October in Lima (UTC-5). */
    private static final Instant START_OF_DAY = Instant.parse("2026-10-30T05:00:00Z");

    @Mock private ExternalSubscriptionService subscriptions;
    @Mock private ExternalOrchardService orchard;
    @Mock private AnswerFactsAssembler factsAssembler;
    @Mock private DailySummaryRepository repository;
    /** Off by default, as a mock answers false: every summary is its template. */
    @Mock private AnswerTextWriter writer;
    /** Lets every summary through by default, as a mock answers an empty OptionalLong. */
    @Mock private AnswerRateLimiter rateLimiter;

    private DailySummaryCommandServiceImpl service;
    private final String producerId = UUID.randomUUID().toString();
    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());

    @BeforeEach
    void setUp() {
        var messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        service = new DailySummaryCommandServiceImpl(subscriptions, orchard, factsAssembler, repository,
                new AnswerLocalizer(messages), new AnswerTextChooser(writer), rateLimiter, CLOCK);
    }

    @Test
    @DisplayName("without plot: the field summary, written from the HOME figures of the whole field with three cards")
    void fieldSummary() {
        var facts = field(openPlot(), openPlot());
        givenActivePlan();
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, null)).thenReturn(facts);

        var outcome = service.handle(command(null, AnswerLanguage.SPANISH, false)).success().orElseThrow();

        assertThat(outcome.created()).isTrue();
        var summary = outcome.summary();
        assertThat(summary.plotId()).isNull();
        assertThat(summary.producerId()).isEqualTo(producerId);
        assertThat(summary.facts()).hasSize(3);
        assertThat(summary.facts().get(0).label()).isEqualTo("días de ventana");
        assertThat(summary.text()).startsWith("En 2 de tus 2 lotes la ventana de aclareo está abierta");
        assertThat(summary.dataVersion()).isEqualTo(AnswerComposer.dataVersionOf(ScreenKey.HOME, facts));
        assertThat(summary.provider()).isEqualTo(AssistantAnswer.TEMPLATE);
        assertThat(summary.fallback()).isFalse();
        assertThat(summary.createdAt()).isEqualTo(NOW);
        verify(repository).save(summary);
        verify(orchard, never()).existsActivePlot(any());
    }

    @Test
    @DisplayName("with plot: the plot is verified and the summary is about that plot, in English")
    void plotSummary() {
        givenActivePlan();
        when(orchard.existsActivePlot(plotId)).thenReturn(true);
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, plotId)).thenReturn(onePlot(openPlot()));

        var summary = service.handle(command(plotId, new AnswerLanguage("en"), false)).success().orElseThrow()
                .summary();

        assertThat(summary.plotId()).isEqualTo(plotId);
        assertThat(summary.language()).isEqualTo(new AnswerLanguage("en"));
        assertThat(summary.facts().get(0)).isEqualTo(
                new AnswerFact("12", "days of window left", "closes Nov 10", FactTone.PRIMARY));
        assertThat(summary.text()).startsWith("Your thinning window is open until November 10");
    }

    @Test
    @DisplayName("the summary of the day with the same figures and language is served again, without limits or a save")
    void servesTheSummaryOfTheDay() {
        var facts = field(openPlot());
        givenActivePlan();
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, null)).thenReturn(facts);
        var stored = stored(false);
        var version = AnswerComposer.dataVersionOf(ScreenKey.HOME, facts);
        when(repository.findCurrent(producerId, null, AnswerLanguage.SPANISH, version, START_OF_DAY))
                .thenReturn(Optional.of(stored));

        var outcome = service.handle(command(null, AnswerLanguage.SPANISH, false)).success().orElseThrow();

        assertThat(outcome.created()).isFalse();
        assertThat(outcome.summary()).isSameAs(stored);
        verify(repository, never()).save(any());
        verifyNoInteractions(rateLimiter, writer);
    }

    @Test
    @DisplayName("only the summaries since midnight in Lima with the current figures count: otherwise a new one")
    void writesAgainWhenNotOfTheDay() {
        var facts = field(openPlot());
        givenActivePlan();
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, null)).thenReturn(facts);

        var outcome = service.handle(command(null, AnswerLanguage.SPANISH, false)).success().orElseThrow();

        assertThat(outcome.created()).isTrue();
        var since = ArgumentCaptor.forClass(Instant.class);
        verify(repository).findCurrent(eq(producerId), isNull(), eq(AnswerLanguage.SPANISH),
                eq(AnswerComposer.dataVersionOf(ScreenKey.HOME, facts)), since.capture());
        assertThat(since.getValue()).isEqualTo(START_OF_DAY);
    }

    @Test
    @DisplayName("a stored summary that fell back to its template is not served again: the model is tried again")
    void fallbackIsNotServedAgain() {
        givenActivePlan();
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, null)).thenReturn(field(openPlot()));
        when(repository.findCurrent(anyString(), any(), any(), any(), any())).thenReturn(Optional.of(stored(true)));

        var outcome = service.handle(command(null, AnswerLanguage.SPANISH, false)).success().orElseThrow();

        assertThat(outcome.created()).isTrue();
        verify(repository).save(outcome.summary());
    }

    @Test
    @DisplayName("model enabled and its text accepted: the summary keeps the model's text")
    void modelTextAccepted() {
        givenActivePlan();
        givenModel();
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, null)).thenReturn(field(openPlot()));
        when(writer.rewrite(any())).thenReturn(Optional.of(new AnswerTextWriter.Written(
                "Tu ventana de aclareo sigue abierta hasta el 10 de noviembre y toca quitar el 30 % de los frutos.",
                120, 40)));

        var summary = service.handle(command(null, AnswerLanguage.SPANISH, false)).success().orElseThrow()
                .summary();

        assertThat(summary.text()).startsWith("Tu ventana de aclareo sigue abierta");
        assertThat(summary.provider()).isEqualTo("GROQ");
        assertThat(summary.modelName()).isEqualTo("openai/gpt-oss-20b");
        assertThat(summary.fallback()).isFalse();
        assertThat(summary.inputTokens()).isEqualTo(120);
    }

    @Test
    @DisplayName("the validator rejects a model text with a figure the server did not compute: the template is kept")
    void validatorRejectionFallsBack() {
        givenActivePlan();
        givenModel();
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, null)).thenReturn(field(openPlot()));
        when(writer.rewrite(any())).thenReturn(Optional.of(new AnswerTextWriter.Written(
                "Tu ventana de aclareo sigue abierta hasta el 10 de noviembre y toca quitar el 45 % de los frutos.",
                120, 40)));

        var summary = service.handle(command(null, AnswerLanguage.SPANISH, false)).success().orElseThrow()
                .summary();

        assertThat(summary.text()).startsWith("Tu ventana de aclareo está abierta hasta el 10 de noviembre");
        assertThat(summary.fallback()).isTrue();
        assertThat(summary.provider()).isEqualTo("GROQ");
    }

    @Test
    @DisplayName("on demand, past the limit of new answers it answers 429 with the wait and nothing is written")
    void onDemandIsLimited() {
        givenActivePlan();
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, null)).thenReturn(field(openPlot()));
        when(rateLimiter.tryAcquire(producerId, false, NOW)).thenReturn(OptionalLong.of(90));

        var error = service.handle(command(null, AnswerLanguage.SPANISH, false)).failure().orElseThrow();

        assertThat(error).isEqualTo(ApplicationError.tooManyRequests("assistant", "assistant.answers.too_many", 90));
        verify(repository, never()).save(any());
        verifyNoInteractions(writer);
    }

    @Test
    @DisplayName("the daily job never spends the producer's limits")
    void scheduledIsNeverLimited() {
        givenActivePlan();
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, null)).thenReturn(field(openPlot()));

        var outcome = service.handle(command(null, AnswerLanguage.SPANISH, true)).success().orElseThrow();

        assertThat(outcome.created()).isTrue();
        verify(rateLimiter, never()).tryAcquire(anyString(), anyBoolean(), any());
    }

    @Test
    @DisplayName("no active plan: 403 before anything is read")
    void inactivePlan() {
        when(subscriptions.hasActivePlan(producerId)).thenReturn(false);

        var error = service.handle(command(plotId, AnswerLanguage.SPANISH, false)).failure().orElseThrow();

        assertThat(error).isEqualTo(ApplicationError.forbidden("assistant", "assistant.subscription.inactive"));
        verifyNoInteractions(orchard, factsAssembler, repository, rateLimiter);
    }

    @Test
    @DisplayName("an unknown or inactive plot: 404, as the catalog answers")
    void unknownPlot() {
        givenActivePlan();
        when(orchard.existsActivePlot(plotId)).thenReturn(false);

        var error = service.handle(command(plotId, AnswerLanguage.SPANISH, false)).failure().orElseThrow();

        assertThat(error).isEqualTo(ApplicationError.notFound("plot", plotId.plotId()));
        verifyNoInteractions(factsAssembler, repository);
    }

    @Test
    @DisplayName("a field without active plots: 409 as the week plan, and nothing is stored or limited")
    void nothingToSummarize() {
        givenActivePlan();
        when(factsAssembler.assemble(ScreenKey.HOME, producerId, null)).thenReturn(field());

        var error = service.handle(command(null, AnswerLanguage.SPANISH, false)).failure().orElseThrow();

        assertThat(error).isEqualTo(ApplicationError.conflict("assistant", "assistant.question.unavailable"));
        verifyNoInteractions(repository, rateLimiter);
    }

    private void givenActivePlan() {
        when(subscriptions.hasActivePlan(producerId)).thenReturn(true);
    }

    private void givenModel() {
        when(writer.enabled()).thenReturn(true);
        when(writer.provider()).thenReturn("GROQ");
        when(writer.model()).thenReturn("openai/gpt-oss-20b");
    }

    private PrepareDailySummaryCommand command(PlotId plot, AnswerLanguage language, boolean scheduled) {
        return new PrepareDailySummaryCommand(producerId, plot, language, scheduled);
    }

    private DailySummary stored(boolean fallback) {
        var card = new AnswerFact("1", "a", "b", FactTone.PRIMARY);
        return new DailySummary(DailySummaryId.generate(), producerId, null, AnswerLanguage.SPANISH,
                new DataVersion("sha256:abc"), "Resumen guardado", List.of(card, card, card), List.of(),
                fallback ? "GROQ" : AssistantAnswer.TEMPLATE, null, fallback, null, null, NOW.minusSeconds(3600));
    }

    /** A plot with the thinning window open 20 Oct - 10 Nov, 30 % to remove, nothing else read. */
    private static HomeFacts.PlotHome openPlot() {
        var plan = new ThinningPlanData(true, 30.0, 0.5714, 0.4, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 10), 5, LocalDate.of(2026, 10, 29), null, null, null, null, null);
        return new HomeFacts.PlotHome(plan, 0, List.of(), null, null, null, null);
    }

    private static HomeFacts onePlot(HomeFacts.PlotHome plot) {
        return new HomeFacts(TODAY, 2026, false, List.of(plot), new ClimateData.Thresholds(32.0, 36.0, 1.5));
    }

    private static HomeFacts field(HomeFacts.PlotHome... plots) {
        return new HomeFacts(TODAY, 2026, true, List.of(plots), new ClimateData.Thresholds(32.0, 36.0, 1.5));
    }
}
