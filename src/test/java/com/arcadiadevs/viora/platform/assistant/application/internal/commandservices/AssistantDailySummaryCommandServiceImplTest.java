package com.arcadiadevs.viora.platform.assistant.application.internal.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantAnswerCommandService.AnswerOutcome;
import com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerFactsEvaluator;
import com.arcadiadevs.viora.platform.assistant.application.limits.AssistantRateLimits;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.DailySummaryFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.infrastructure.ratelimit.SlidingWindowAssistantRateLimits;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture.PRODUCER;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.bare;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.field;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.withAlerts;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.withPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AssistantDailySummaryCommandServiceImpl")
class AssistantDailySummaryCommandServiceImplTest {

    private static final String SUMMARY = QuestionId.HOME_DAILY_SUMMARY.value();
    private final AssistantServiceFixture fixture = new AssistantServiceFixture();
    private final AnswerFactsEvaluator evaluator = new AnswerFactsEvaluator(fixture.catalog, fixture.dataReader,
            new FactsAssemblerRegistry(List.of(new DailySummaryFactsAssembler())));

    AssistantDailySummaryCommandServiceImplTest() {
        fixture.givenCatalogFacts(PlotFacts.ofHome(2026, new HomeData(true, List.of(bare(), bare()), null)));
        fixture.givenData(field(withPlan(issuedPlan(), 0), bare()));
    }

    @Test
    @DisplayName("Generates the summary once with three facts, then serves it while its data has not changed")
    void generatesOnceThenServesIt() {
        var service = service(fixture.rateLimits(20, 5));

        var first = service.handle(onDemand()).success().orElseThrow();
        var second = service.handle(onDemand()).success().orElseThrow();

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        var answer = first.view().answer();
        assertThat(second.view().answer().id()).isEqualTo(answer.id());
        assertThat(answer.questionId()).isEqualTo(QuestionId.HOME_DAILY_SUMMARY);
        assertThat(answer.plotId()).isNull();
        assertThat(answer.producerId()).isEqualTo(PRODUCER);
        assertThat(answer.locale()).isEqualTo("es");
        assertThat(answer.facts()).hasSize(3);
        assertThat(answer.facts().getFirst().value()).isEqualTo("12");
        assertThat(answer.facts().getFirst().label()).isEqualTo("días de ventana");
        assertThat(first.view().freshness()).isEqualTo(Freshness.UP_TO_DATE);
        assertThat(second.view().freshness()).isEqualTo(Freshness.UP_TO_DATE);
        assertThat(fixture.repository.all()).hasSize(1);
        verify(fixture.writer, times(1)).write(any());
        assertThat(fixture.answers(SUMMARY, "created")).isEqualTo(1);
        assertThat(fixture.answers(SUMMARY, "cache")).isEqualTo(1);
    }

    @Test
    @DisplayName("A stored summary whose data changed is regenerated, never served as current")
    void regeneratedWhenStale() {
        var service = service(fixture.rateLimits(20, 5));
        var first = service.handle(onDemand()).success().orElseThrow();

        fixture.givenData(field(withPlan(issuedPlan(), 0), withAlerts(heat())));
        var second = service.handle(onDemand()).success().orElseThrow();

        assertThat(second.created()).isTrue();
        assertThat(second.view().answer().id()).isNotEqualTo(first.view().answer().id());
        assertThat(second.view().answer().dataVersion()).isNotEqualTo(first.view().answer().dataVersion());
        assertThat(second.view().answer().facts().get(2).value()).isEqualTo("1");
        assertThat(fixture.repository.all()).hasSize(2);
    }

    @Test
    @DisplayName("A summary of yesterday (in Lima) is regenerated even with the same data")
    void oneSummaryPerDay() {
        var service = service(fixture.rateLimits(20, 5));
        var first = service.handle(onDemand()).success().orElseThrow();

        // 15:00Z is 10:00 in Lima; 13 hours later it is 23:00, still the same day there
        fixture.clock.advance(Duration.ofHours(13));
        assertThat(service.handle(onDemand()).success().orElseThrow().view().answer().id())
                .isEqualTo(first.view().answer().id());
        fixture.clock.advance(Duration.ofHours(2));
        var nextDay = service.handle(onDemand()).success().orElseThrow();

        assertThat(nextDay.created()).isTrue();
        assertThat(nextDay.view().answer().id()).isNotEqualTo(first.view().answer().id());
    }

    @Test
    @DisplayName("Generating on demand counts against the answers limit; serving the stored summary does not")
    void onDemandGenerationIsLimited() {
        var service = service(fixture.rateLimits(1, 5));
        service.handle(onDemand()).success().orElseThrow();
        assertThat(service.handle(onDemand()).success()).isPresent();

        fixture.givenData(field(withPlan(issuedPlan(), 0), withAlerts(heat())));
        Result<AnswerOutcome, ApplicationError> limited = service.handle(onDemand());

        assertThat(limited.failure().map(ApplicationError::code)).contains("ASSISTANT_TOO_MANY_REQUESTS");
        assertThat(limited.failure().map(ApplicationError::details)).contains("assistant.rate_limited");
        assertThat(fixture.repository.all()).hasSize(1);
        assertThat(fixture.answers(SUMMARY, "rate_limited")).isEqualTo(1);
    }

    @Test
    @DisplayName("The daily job never spends the producer's limits")
    void scheduledNeverCounts() {
        var service = service(new SlidingWindowAssistantRateLimits(1, 1, 1, fixture.clock));

        assertThat(service.handle(scheduled()).success().orElseThrow().created()).isTrue();
        fixture.givenData(field(withPlan(issuedPlan(), 0), withAlerts(heat())));
        assertThat(service.handle(scheduled()).success().orElseThrow().created()).isTrue();
        // The producer still has its whole quota: one read and one generation
        fixture.givenData(field(withPlan(issuedPlan(), 0), withAlerts(heat(), heat())));
        assertThat(service.handle(onDemand()).success().orElseThrow().created()).isTrue();
        assertThat(fixture.repository.all()).hasSize(3);
    }

    @Test
    @DisplayName("Every request reads the facts, so it counts against the catalog reads limit")
    void readsAreLimited() {
        var service = service(new SlidingWindowAssistantRateLimits(20, 5, 1, fixture.clock));
        service.handle(onDemand()).success().orElseThrow();

        var limited = service.handle(onDemand());

        assertThat(limited.failure().map(ApplicationError::code)).contains("ASSISTANT_TOO_MANY_REQUESTS");
        assertThat(service.handle(scheduled()).success()).isPresent();
    }

    @Test
    @DisplayName("A template summary while a model writes is served again only within the fallback TTL")
    void expiredFallbackIsRegenerated() {
        doReturn(true).when(fixture.writer).usesModel();
        var service = service(fixture.rateLimits(20, 5));
        var first = service.handle(onDemand()).success().orElseThrow();

        fixture.clock.advance(Duration.ofMinutes(9));
        assertThat(service.handle(onDemand()).success().orElseThrow().view().answer().id())
                .isEqualTo(first.view().answer().id());
        fixture.clock.advance(Duration.ofMinutes(2));

        assertThat(service.handle(onDemand()).success().orElseThrow().created()).isTrue();
    }

    @Test
    @DisplayName("No active plan: 403; no active plots: 409 with the reason of the week plan; nothing stored")
    void refused() {
        var service = service(fixture.rateLimits(20, 5));
        fixture.givenCatalogFacts(PlotFacts.ofHome(2026, new HomeData(true, List.of(), null)));

        var noPlots = service.handle(onDemand());
        when(fixture.subscription.hasActivePlan(anyString())).thenReturn(false);
        var inactive = service.handle(onDemand());

        assertThat(noPlots.failure().map(ApplicationError::code)).contains("ASSISTANT_QUESTION_CONFLICT");
        assertThat(noPlots.failure().map(ApplicationError::details)).contains("Sin lotes activos · registra un lote");
        assertThat(inactive.failure().map(ApplicationError::code)).contains("ASSISTANT_FORBIDDEN");
        assertThat(inactive.failure().map(ApplicationError::details)).contains("assistant.subscription.inactive");
        assertThat(fixture.repository.all()).isEmpty();
        assertThat(fixture.answers(SUMMARY, "unavailable")).isEqualTo(1);
    }

    private AssistantDailySummaryCommandServiceImpl service(AssistantRateLimits limits) {
        return new AssistantDailySummaryCommandServiceImpl(fixture.subscription, evaluator, fixture.renderer,
                fixture.writer, fixture.repository, limits, fixture.metrics, fixture.clock, Duration.ofMinutes(10));
    }

    private static PrepareDailySummaryCommand onDemand() {
        return new PrepareDailySummaryCommand(PRODUCER, null, Locale.forLanguageTag("es"), false);
    }

    private static PrepareDailySummaryCommand scheduled() {
        return new PrepareDailySummaryCommand(PRODUCER, null, Locale.forLanguageTag("es"), true);
    }
}
