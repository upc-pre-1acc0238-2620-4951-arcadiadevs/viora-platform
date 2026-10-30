package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerByIdQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerHistoryQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetLatestAnswerQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyPercentFactsAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture.PRODUCER;
import static com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture.T0;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AssistantAnswerQueryService: history, reopen and latest")
class AssistantAnswerQueryServiceImplTest {

    private static final List<AnswerFact> CARDS = List.of(new AnswerFact("0,57", "a", "b", FactTone.PRIMARY),
            new AnswerFact("−30 %", "c", "d", FactTone.HIGHLIGHT));

    private AssistantServiceFixture fixture;
    private final ExternalOrchardService orchard = mock(ExternalOrchardService.class);
    private AssistantAnswerQueryServiceImpl service;
    private DataVersion current;

    @BeforeEach
    void setUp() {
        fixture = new AssistantServiceFixture();
        when(orchard.existsActivePlot(any())).thenReturn(true);
        service = new AssistantAnswerQueryServiceImpl(fixture.subscription, orchard, fixture.evaluator,
                fixture.repository, fixture.clock);
        current = new WhyPercentFactsAssembler().assemble(plan(issuedPlan())).orElseThrow().dataVersion();
    }

    @Test
    @DisplayName("Lists at most the limit, newest first, only the producer's and only from the last 10 days")
    void listsTheRecentAnswers() {
        for (int hour = 0; hour < 12; hour++) {
            store(PRODUCER, QuestionId.PLAN_WHY_PERCENT, current, T0.minus(Duration.ofHours(hour)));
        }
        var expired = store(PRODUCER, QuestionId.PLAN_WHY_PERCENT, current,
                T0.minus(AssistantAnswer.RETENTION).minusSeconds(1));
        var foreign = store("another-producer", QuestionId.PLAN_WHY_PERCENT, current, T0);

        var ten = service.handle(new GetAnswerHistoryQuery(PRODUCER, 10)).success().orElseThrow();
        var three = service.handle(new GetAnswerHistoryQuery(PRODUCER, 3)).success().orElseThrow();

        assertThat(ten).hasSize(10);
        assertThat(ten).extracting(view -> view.answer().createdAt()).isSortedAccordingTo(
                java.util.Comparator.reverseOrder());
        assertThat(ten.getFirst().answer().createdAt()).isEqualTo(T0);
        assertThat(ten).extracting(view -> view.answer().id()).doesNotContain(expired.id(), foreign.id());
        assertThat(three).hasSize(3);
        assertThatThrownBy(() -> new GetAnswerHistoryQuery(PRODUCER, 11)).hasMessage("assistant.limit.invalid");
        assertThatThrownBy(() -> new GetAnswerHistoryQuery(PRODUCER, 0)).hasMessage("assistant.limit.invalid");
    }

    @Test
    @DisplayName("Each answer is up to date while its facts match, stale when they changed or it became unavailable")
    void computesTheFreshness() {
        var upToDate = store(PRODUCER, QuestionId.PLAN_WHY_PERCENT, current, T0);
        var changed = store(PRODUCER, QuestionId.PLAN_WHY_PERCENT, DataVersion.of(Map.of("percentToRemove", 20)),
                T0.minusSeconds(60));

        var views = service.handle(new GetAnswerHistoryQuery(PRODUCER, 10)).success().orElseThrow();
        fixture.givenCatalogFacts(AssistantServiceFixture.withoutPlan());
        var afterLosingThePlan = service.handle(new GetAnswerByIdQuery(PRODUCER, upToDate.id())).success()
                .orElseThrow();

        assertThat(views).extracting(view -> view.answer().id(), AnswerView::freshness).containsExactly(
                org.assertj.core.groups.Tuple.tuple(upToDate.id(), Freshness.UP_TO_DATE),
                org.assertj.core.groups.Tuple.tuple(changed.id(), Freshness.STALE));
        assertThat(afterLosingThePlan.freshness()).isEqualTo(Freshness.STALE);
    }

    @Test
    @DisplayName("Reopens only the producer's own answers of the last 10 days; any other is not found")
    void reopensAnAnswer() {
        var own = store(PRODUCER, QuestionId.PLAN_WHY_PERCENT, current, T0);
        var foreign = store("another-producer", QuestionId.PLAN_WHY_PERCENT, current, T0);
        var old = store(PRODUCER, QuestionId.PLAN_WHY_PERCENT, current, T0.minus(Duration.ofDays(11)));

        var found = service.handle(new GetAnswerByIdQuery(PRODUCER, own.id()));

        assertThat(found.success().map(view -> view.answer().id())).contains(own.id());
        for (var hidden : List.of(foreign.id(), old.id(), UUID.randomUUID())) {
            assertThat(service.handle(new GetAnswerByIdQuery(PRODUCER, hidden)).failure()
                    .map(ApplicationError::code)).contains("ASSISTANT_ANSWER_NOT_FOUND");
        }
    }

    @Test
    @DisplayName("Latest answer of the screen and plot, or none")
    void findsTheLatestOfTheScreen() {
        assertThat(service.handle(new GetLatestAnswerQuery(PRODUCER, ScreenKey.PLAN, PLOT)).success())
                .contains(java.util.Optional.empty());

        store(PRODUCER, QuestionId.PLAN_IF_LESS, current, T0.minusSeconds(120));
        var latest = store(PRODUCER, QuestionId.PLAN_WHY_PERCENT, current, T0.minusSeconds(60));
        store(PRODUCER, QuestionId.ALTERNATION_WHY_INDEX, current, T0);

        var found = service.handle(new GetLatestAnswerQuery(PRODUCER, ScreenKey.PLAN, PLOT)).success().orElseThrow();

        assertThat(found.map(view -> view.answer().id())).contains(latest.id());
        assertThat(service.handle(new GetLatestAnswerQuery(PRODUCER, ScreenKey.ALTERNATION,
                new com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId(
                        UUID.randomUUID().toString()))).success()).contains(java.util.Optional.empty());
    }

    @Test
    @DisplayName("Every read requires an active plan")
    void requiresAnActivePlan() {
        when(fixture.subscription.hasActivePlan(PRODUCER)).thenReturn(false);

        assertThat(service.handle(new GetAnswerHistoryQuery(PRODUCER, 10)).failure().map(ApplicationError::code))
                .contains("ASSISTANT_FORBIDDEN");
        assertThat(service.handle(new GetAnswerByIdQuery(PRODUCER, UUID.randomUUID())).failure()
                .map(ApplicationError::code)).contains("ASSISTANT_FORBIDDEN");
        assertThat(service.handle(new GetLatestAnswerQuery(PRODUCER, ScreenKey.PLAN, PLOT)).failure()
                .map(ApplicationError::code)).contains("ASSISTANT_FORBIDDEN");
    }

    private AssistantAnswer store(String producer, QuestionId questionId, DataVersion version, Instant createdAt) {
        return fixture.repository.save(AssistantAnswer.create(UUID.randomUUID(), producer, PLOT, questionId, "es",
                version, "Texto.", CARDS, List.of(), List.of(), AnswerModel.TEMPLATE, createdAt));
    }
}
