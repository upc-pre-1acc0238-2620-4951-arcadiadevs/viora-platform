package com.arcadiadevs.viora.platform.assistant.application.internal;

import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerFactsEvaluator;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerRenderer;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.PlotAnswerDataReader;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.TemplateAnswerWriter;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.application.queryservices.QuestionCatalogQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import com.arcadiadevs.viora.platform.assistant.domain.services.QuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyIndexFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyPercentFactsAssembler;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.Instant;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Collaborators of the answer services: the real evaluator, renderer and assemblers (spied), with the catalog, the
 * plot data and the plan mocked, an in-memory repository and a clock moved by hand.
 */
public class AssistantServiceFixture {

    public static final String PRODUCER = "6f1c2a10-0000-4000-8000-000000000001";
    public static final Instant T0 = Instant.parse("2026-10-30T15:00:00Z");

    public final ExternalSubscriptionService subscription = mock(ExternalSubscriptionService.class);
    public final QuestionCatalogQueryService catalog = mock(QuestionCatalogQueryService.class);
    public final PlotAnswerDataReader dataReader = mock(PlotAnswerDataReader.class);
    public final FactsAssembler whyPercent = spy(new WhyPercentFactsAssembler());
    public final FactsAssembler whyIndex = spy(new WhyIndexFactsAssembler());
    public final InMemoryAssistantAnswerRepository repository = new InMemoryAssistantAnswerRepository();
    public final MutableClock clock = new MutableClock(T0);
    public final AnswerRenderer renderer = new AnswerRenderer(messageSource());
    /** The template writer, spied to prove when a writer (and so a model) is called. */
    public final TemplateAnswerWriter writer = spy(new TemplateAnswerWriter());
    public final AnswerFactsEvaluator evaluator = new AnswerFactsEvaluator(catalog, dataReader,
            new FactsAssemblerRegistry(List.of(whyPercent, whyIndex)));

    public AssistantServiceFixture() {
        when(subscription.hasActivePlan(anyString())).thenReturn(true);
        givenCatalogFacts(enabledFacts());
        givenData(plan(issuedPlan()));
    }

    /** The catalog evaluates its questions with these facts, as the real query service does. */
    public void givenCatalogFacts(PlotFacts facts) {
        doAnswer(invocation -> {
            com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery query =
                    invocation.getArgument(0);
            return Result.success(new ScreenQuestionCatalog(query.screen(), query.plotId(),
                    QuestionCatalog.questionsFor(query.screen(), facts)));
        }).when(catalog).handle(any());
    }

    public void givenData(PlotAnswerData data) {
        doReturn(data).when(dataReader).read(any(), any());
    }

    /** Every ALTERNATION and PLAN question enabled. */
    public static PlotFacts enabledFacts() {
        return new PlotFacts(2026, 0.51, 3, true, true, 30.0, 5, 0);
    }

    /** No prescription: every PLAN question disabled with "Sin plan de aclareo activo". */
    public static PlotFacts withoutPlan() {
        return new PlotFacts(2026, 0.51, 3, false, false, null, 0, 0);
    }

    private static ResourceBundleMessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
