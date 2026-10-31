package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionCatalogQueryService Unit Tests")
class QuestionCatalogQueryServiceImplTest {

    /** Noon in Lima on 2026-06-15, so the current campaign is 2026 and the next one 2027. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T17:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ExternalSubscriptionService externalSubscriptionService;
    @Mock
    private ExternalOrchardService externalOrchardService;
    @Mock
    private ExternalPhenologyService externalPhenologyService;
    @Mock
    private ExternalThinningService externalThinningService;
    @Mock
    private ExternalTelemetryService externalTelemetryService;

    private QuestionCatalogQueryServiceImpl queryService;

    private final String producerId = UUID.randomUUID().toString();
    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());

    @BeforeEach
    void setUp() {
        var factsAssembler = new AnswerFactsAssembler(List.of(
                new HomeFactsReader(externalOrchardService, externalThinningService, externalPhenologyService,
                        externalTelemetryService, CLOCK),
                new AlternationFactsReader(externalOrchardService, externalPhenologyService, externalThinningService),
                new PlanFactsReader(externalThinningService),
                new AlertFactsReader(externalTelemetryService, externalOrchardService),
                new ClimateFactsReader(externalTelemetryService, CLOCK),
                new ChillFactsReader(externalPhenologyService),
                new PlotDetailFactsReader(externalThinningService, externalPhenologyService, externalTelemetryService,
                        CLOCK),
                new SensorsFactsReader(externalTelemetryService, CLOCK),
                new SamplingFactsReader(externalThinningService, CLOCK),
                new ThinningDoneFactsReader(externalThinningService),
                new CampaignClosedFactsReader(externalPhenologyService, externalThinningService,
                        externalOrchardService)), CLOCK);
        queryService = new QuestionCatalogQueryServiceImpl(externalSubscriptionService, externalOrchardService,
                factsAssembler);
    }

    @Test
    @DisplayName("ALTERNATION: every question enabled, in contract order, with the index and next year")
    void alternationWithEveryFact() {
        givenAccessiblePlot();
        when(externalPhenologyService.findBearing(plotId)).thenReturn(Optional.of(bearing(0.51)));
        when(externalPhenologyService.findHistoricalYields(plotId)).thenReturn(harvests(3));
        when(externalThinningService.findPrescriptionAvailability(plotId, 2026))
                .thenReturn(Optional.of(new PrescriptionAvailability(true, 30.0, 5, 0, "ESTIMATED")));

        var questions = catalog(ScreenKey.ALTERNATION).questions();

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "alternation.why_index", "alternation.break_cycle", "alternation.vs_campaigns",
                "alternation.next_year");
        assertThat(questions).extracting(CatalogQuestion::icon)
                .containsExactly("calendar", "pruning", "history", "harvest");
        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions).allMatch(q -> q.disabledReason() == null);
        assertThat(questions.get(0).label()).isEqualTo(CatalogText.of("assistant.question.alternation.why_index.label", 0.51));
        assertThat(questions.get(3).label()).isEqualTo(CatalogText.of("assistant.question.alternation.next_year.label", "2027"));
    }

    @Test
    @DisplayName("ALTERNATION: without index, history or caliber every question is disabled with its reason")
    void alternationWithoutFacts() {
        givenAccessiblePlot();
        when(externalPhenologyService.findBearing(plotId)).thenReturn(Optional.empty());
        when(externalPhenologyService.findHistoricalYields(plotId)).thenReturn(harvests(1));
        when(externalThinningService.findPrescriptionAvailability(plotId, 2026)).thenReturn(Optional.empty());

        var questions = catalog(ScreenKey.ALTERNATION).questions();

        assertThat(questions).noneMatch(CatalogQuestion::enabled);
        assertThat(questions.get(0).label())
                .isEqualTo(CatalogText.of("assistant.question.alternation.why_index.label_without_index"));
        assertThat(questions).extracting(CatalogQuestion::disabledReason).containsExactly(
                CatalogText.of("assistant.reason.bbi_missing"),
                CatalogText.of("assistant.reason.bbi_missing"),
                CatalogText.of("assistant.reason.campaigns_missing"),
                CatalogText.of("assistant.reason.caliber_missing"));
        assertThat(questions.get(3).label()).isEqualTo(CatalogText.of("assistant.question.alternation.next_year.label", "2027"));
    }

    @Test
    @DisplayName("ALTERNATION: two campaigns are enough to compare; a caliber not estimated still blocks next year")
    void alternationWithTwoCampaignsAndNoCaliberEstimate() {
        givenAccessiblePlot();
        when(externalPhenologyService.findBearing(plotId)).thenReturn(Optional.of(bearing(0.12)));
        when(externalPhenologyService.findHistoricalYields(plotId)).thenReturn(harvests(2));
        when(externalThinningService.findPrescriptionAvailability(plotId, 2026))
                .thenReturn(Optional.of(new PrescriptionAvailability(true, 30.0, 5, 0, "NOT_CALIBRATED")));

        var questions = catalog(ScreenKey.ALTERNATION).questions();

        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, true, true, false);
        assertThat(questions.get(3).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.caliber_missing"));
    }

    @Test
    @DisplayName("PLAN: an issued prescription enables every question with its percent and 10 points less")
    void planWithIssuedPrescription() {
        givenAccessiblePlot();
        when(externalThinningService.findPrescriptionAvailability(plotId, 2026))
                .thenReturn(Optional.of(new PrescriptionAvailability(true, 30.0, 5, 0, null)));

        var questions = catalog(ScreenKey.PLAN).questions();

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "plan.why_percent", "plan.if_less", "plan.if_none", "plan.best_time");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("prio", "pruning", "alert", "calendar");
        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions.get(0).label()).isEqualTo(CatalogText.of("assistant.question.plan.why_percent.label", 30));
        assertThat(questions.get(1).label()).isEqualTo(CatalogText.of("assistant.question.plan.if_less.label", 20));
        verify(externalPhenologyService, never()).findBiennialBearingIndex(any());
        verify(externalPhenologyService, never()).findBearing(any());
    }

    @Test
    @DisplayName("PLAN: ten points less floors at zero and then drops the figure")
    void planIfLessFloorsAtZero() {
        givenAccessiblePlot();
        when(externalThinningService.findPrescriptionAvailability(plotId, 2026))
                .thenReturn(Optional.of(new PrescriptionAvailability(true, 8.0, 5, 0, null)));

        var questions = catalog(ScreenKey.PLAN).questions();

        assertThat(questions.get(0).label()).isEqualTo(CatalogText.of("assistant.question.plan.why_percent.label", 8));
        assertThat(questions.get(1).label())
                .isEqualTo(CatalogText.of("assistant.question.plan.if_less.label_without_percent"));
        assertThat(questions.get(1).enabled()).isTrue();
    }

    @Test
    @DisplayName("PLAN: an incomplete sampling disables every question with the trees still missing")
    void planWithIncompleteSampling() {
        givenAccessiblePlot();
        when(externalThinningService.findPrescriptionAvailability(plotId, 2026))
                .thenReturn(Optional.of(new PrescriptionAvailability(false, null, 2, 3, null)));

        var questions = catalog(ScreenKey.PLAN).questions();

        assertThat(questions).noneMatch(CatalogQuestion::enabled);
        assertThat(questions).allMatch(q -> q.disabledReason().equals(CatalogText.of("assistant.reason.trees_missing.other", 3)));
        assertThat(questions.get(0).label())
                .isEqualTo(CatalogText.of("assistant.question.plan.why_percent.label_without_percent"));
        assertThat(questions.get(1).label())
                .isEqualTo(CatalogText.of("assistant.question.plan.if_less.label_without_percent"));
    }

    @Test
    @DisplayName("PLAN: one tree missing uses the singular reason")
    void planWithOneTreeMissing() {
        givenAccessiblePlot();
        when(externalThinningService.findPrescriptionAvailability(plotId, 2026))
                .thenReturn(Optional.of(new PrescriptionAvailability(false, null, 4, 1, null)));

        assertThat(catalog(ScreenKey.PLAN).questions())
                .allMatch(q -> q.disabledReason().equals(CatalogText.of("assistant.reason.trees_missing.one")));
    }

    @Test
    @DisplayName("PLAN: without prescription nor sampling there is no active plan")
    void planWithoutPrescription() {
        givenAccessiblePlot();
        when(externalThinningService.findPrescriptionAvailability(plotId, 2026)).thenReturn(Optional.empty());

        assertThat(catalog(ScreenKey.PLAN).questions())
                .allMatch(q -> !q.enabled() && q.disabledReason().equals(CatalogText.of("assistant.reason.plan_missing")));
    }

    @Test
    @DisplayName("Without an active plan answers 403 before reading any plot data")
    void rejectsWithoutActivePlan() {
        when(externalSubscriptionService.hasActivePlan(producerId)).thenReturn(false);

        var result = queryService.handle(new GetQuestionCatalogQuery(producerId, ScreenKey.PLAN, plotId));

        var error = result.failure().orElseThrow();
        assertThat(error.code()).isEqualTo("ASSISTANT_FORBIDDEN");
        assertThat(error.details()).isEqualTo("assistant.subscription.inactive");
        verify(externalOrchardService, never()).existsActivePlot(any());
        verify(externalThinningService, never()).findPrescriptionAvailability(any(), anyInt());
    }

    @Test
    @DisplayName("An unknown or inactive plot answers 404")
    void rejectsUnknownPlot() {
        when(externalSubscriptionService.hasActivePlan(producerId)).thenReturn(true);
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(false);

        var error = queryService.handle(new GetQuestionCatalogQuery(producerId, ScreenKey.ALTERNATION, plotId))
                .failure().orElseThrow();

        assertThat(error.code()).isEqualTo("PLOT_NOT_FOUND");
        verify(externalPhenologyService, never()).findBiennialBearingIndex(any());
        verify(externalPhenologyService, never()).findBearing(any());
    }

    @Test
    @DisplayName("Rejects an unknown screen, a missing plot and a malformed plot id")
    void rejectsInvalidInput() {
        assertThatThrownBy(() -> ScreenKey.fromValue("HOME_SCREEN")).hasMessage("assistant.screen.invalid");
        assertThatThrownBy(() -> ScreenKey.fromValue(null)).hasMessage("assistant.screen.invalid");
        assertThatThrownBy(() -> new GetQuestionCatalogQuery(producerId, ScreenKey.PLAN, null))
                .hasMessage("assistant.plot.required");
        assertThatThrownBy(() -> new PlotId("not-a-uuid")).hasMessage("assistant.plot.invalid");
        assertThat(ScreenKey.fromValue("alternation")).isEqualTo(ScreenKey.ALTERNATION);
    }

    private static PlotBearingIndex bearing(double value) {
        return new PlotBearingIndex(value, "MODERATE_ALTERNATION");
    }

    /** Harvests of the given number of campaigns, the last one in 2025. */
    private static SortedMap<Integer, Double> harvests(int campaigns) {
        var yields = new TreeMap<Integer, Double>();
        for (int i = 0; i < campaigns; i++) {
            yields.put(2025 - i, 50_000.0);
        }
        return yields;
    }

    private void givenAccessiblePlot() {
        when(externalSubscriptionService.hasActivePlan(producerId)).thenReturn(true);
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(true);
    }

    private ScreenQuestionCatalog catalog(ScreenKey screen) {
        var result = queryService.handle(new GetQuestionCatalogQuery(producerId, screen, plotId));
        var catalog = result.success().orElseThrow();
        assertThat(catalog.screen()).isEqualTo(screen);
        assertThat(catalog.plotId()).isEqualTo(plotId);
        assertThat(catalog.questions()).hasSize(4);
        return catalog;
    }
}
