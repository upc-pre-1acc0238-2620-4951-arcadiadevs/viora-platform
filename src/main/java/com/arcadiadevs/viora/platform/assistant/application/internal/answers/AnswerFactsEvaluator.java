package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.application.queryservices.QuestionCatalogQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import com.arcadiadevs.viora.platform.assistant.domain.services.QuestionCatalog;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Decides whether a question can be answered now and, if so, builds its facts. Availability is the catalog's own
 * evaluation (plan, plot and data checks), so a question offered disabled is never answered and its assembler is
 * never called. The daily summary of HOME ({@code home.daily_summary}) is not in the catalog: it is available exactly
 * when {@code home.week_plan} is, since it summarizes the same facts.
 */
@Component
public class AnswerFactsEvaluator {

    /** Reason when the catalog enables a question but a figure its answer needs is still missing. */
    static final String FACTS_MISSING = "assistant.reason.facts_missing";

    private final QuestionCatalogQueryService catalogQueryService;
    private final PlotAnswerDataReader dataReader;
    private final FactsAssemblerRegistry assemblers;

    public AnswerFactsEvaluator(QuestionCatalogQueryService catalogQueryService, PlotAnswerDataReader dataReader,
            FactsAssemblerRegistry assemblers) {
        this.catalogQueryService = catalogQueryService;
        this.dataReader = dataReader;
        this.assemblers = assemblers;
    }

    /**
     * Evaluates a question about a plot.
     *
     * @param producerId producer asking
     * @param questionId the question, or the daily summary
     * @param plotId     the plot; null for a HOME question about the producer's whole field
     * @return the assessment, or the catalog error (no active plan, unknown plot, unknown question)
     */
    public Result<AnswerAssessment, ApplicationError> assess(String producerId, QuestionId questionId,
            PlotId plotId) {
        // The daily summary is not offered in the catalog: it is a HOME answer available when the week plan is
        boolean summary = QuestionId.HOME_DAILY_SUMMARY.equals(questionId);
        var screen = summary ? Optional.of(ScreenKey.HOME) : QuestionCatalog.screenOf(questionId);
        var offered = summary ? QuestionId.HOME_WEEK_PLAN : questionId;
        if (screen.isEmpty()) {
            return Result.failure(ApplicationError.validationError("questionId", "assistant.question.invalid"));
        }
        return catalogQueryService.handle(new GetQuestionCatalogQuery(producerId, screen.get(), plotId))
                .map(catalog -> {
                    var question = catalog.questions().stream()
                            .filter(candidate -> candidate.questionId().equals(offered))
                            .findFirst();
                    if (question.isEmpty()) {
                        return new AnswerAssessment.Unavailable(CatalogText.of(FACTS_MISSING));
                    }
                    if (!question.get().enabled()) {
                        return new AnswerAssessment.Unavailable(question.get().disabledReason());
                    }
                    var assembler = assemblers.find(questionId);
                    if (assembler.isEmpty()) {
                        return new AnswerAssessment.Unavailable(CatalogText.of(FACTS_MISSING));
                    }
                    return assembler.get().assemble(dataReader.read(screen.get(), producerId, plotId))
                            .<AnswerAssessment>map(AnswerAssessment.Available::new)
                            .orElseGet(() -> new AnswerAssessment.Unavailable(CatalogText.of(FACTS_MISSING)));
                });
    }

    /**
     * Compares a stored answer with the current facts of its question.
     *
     * @param answer the answer
     * @return stale when the facts changed or the question cannot be answered anymore
     */
    public Freshness freshnessOf(AssistantAnswer answer) {
        Optional<DataVersion> current = assess(answer.producerId(), answer.questionId(), answer.plotId()).success()
                .flatMap(assessment -> assessment instanceof AnswerAssessment.Available available
                        ? Optional.of(available.facts().dataVersion())
                        : Optional.empty());
        return answer.freshnessAgainst(current);
    }
}
