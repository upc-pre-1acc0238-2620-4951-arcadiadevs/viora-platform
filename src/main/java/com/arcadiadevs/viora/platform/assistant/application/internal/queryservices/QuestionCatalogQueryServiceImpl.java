package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.application.queryservices.QuestionCatalogQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.domain.services.QuestionCatalog;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

/**
 * Application query service evaluating the question catalog of a screen against the data of the plot. It only
 * reads figures other contexts already computed, the same ones the answers are written from, so the catalog stays
 * cheap and never calls the model.
 */
@Service
public class QuestionCatalogQueryServiceImpl implements QuestionCatalogQueryService {

    private final ExternalSubscriptionService externalSubscriptionService;
    private final ExternalOrchardService externalOrchardService;
    private final AnswerFactsAssembler factsAssembler;

    /**
     * Constructs the query service.
     *
     * @param externalSubscriptionService tells whether the producer's plan is active
     * @param externalOrchardService      verifies the plot
     * @param factsAssembler              reads the figures of the screen
     */
    public QuestionCatalogQueryServiceImpl(
            ExternalSubscriptionService externalSubscriptionService,
            ExternalOrchardService externalOrchardService,
            AnswerFactsAssembler factsAssembler
    ) {
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalOrchardService = externalOrchardService;
        this.factsAssembler = factsAssembler;
    }

    @Override
    public Result<ScreenQuestionCatalog, ApplicationError> handle(GetQuestionCatalogQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "assistant.query.null"));
        }
        if (!externalSubscriptionService.hasActivePlan(query.producerId())) {
            return Result.failure(ApplicationError.forbidden("assistant", "assistant.subscription.inactive"));
        }
        if (!externalOrchardService.existsActivePlot(query.plotId())) {
            return Result.failure(ApplicationError.notFound("plot", query.plotId().plotId()));
        }
        var facts = factsAssembler.assemble(query.screen(), query.plotId());
        return Result.success(new ScreenQuestionCatalog(query.screen(), query.plotId(),
                QuestionCatalog.questionsFor(facts)));
    }
}
