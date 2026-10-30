package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerFactsEvaluator;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.application.queryservices.AssistantAnswerQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerByIdQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerHistoryQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetLatestAnswerQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Reads the stored answers of a producer: only its own, only while they are kept (10 days), each compared with the
 * current facts of its question.
 */
@Service
public class AssistantAnswerQueryServiceImpl implements AssistantAnswerQueryService {

    private final ExternalSubscriptionService externalSubscriptionService;
    private final ExternalOrchardService externalOrchardService;
    private final AnswerFactsEvaluator factsEvaluator;
    private final AssistantAnswerRepository repository;
    private final Clock clock;

    public AssistantAnswerQueryServiceImpl(ExternalSubscriptionService externalSubscriptionService,
            ExternalOrchardService externalOrchardService, AnswerFactsEvaluator factsEvaluator,
            AssistantAnswerRepository repository, Clock clock) {
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalOrchardService = externalOrchardService;
        this.factsEvaluator = factsEvaluator;
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public Result<List<AnswerView>, ApplicationError> handle(GetAnswerHistoryQuery query) {
        if (!externalSubscriptionService.hasActivePlan(query.producerId())) {
            return inactivePlan();
        }
        var answers = repository.findRecent(query.producerId(), clock.instant().minus(AssistantAnswer.RETENTION),
                query.limit());
        // Answers to the same question about the same plot share their current facts
        var freshness = new HashMap<String, Freshness>();
        return Result.success(answers.stream()
                .map(answer -> new AnswerView(answer, freshness.computeIfAbsent(
                        answer.questionId().value() + "|" + answer.plotId().plotId() + "|" + answer.dataVersion().value(),
                        key -> factsEvaluator.freshnessOf(answer))))
                .toList());
    }

    @Override
    public Result<AnswerView, ApplicationError> handle(GetAnswerByIdQuery query) {
        if (!externalSubscriptionService.hasActivePlan(query.producerId())) {
            return inactivePlan();
        }
        var now = clock.instant();
        return repository.findById(query.answerId())
                .filter(answer -> answer.producerId().equalsIgnoreCase(query.producerId()))
                .filter(answer -> answer.isRetainedAt(now))
                .map(answer -> Result.<AnswerView, ApplicationError>success(
                        new AnswerView(answer, factsEvaluator.freshnessOf(answer))))
                .orElseGet(() -> Result.failure(
                        ApplicationError.notFound("assistant_answer", query.answerId().toString())));
    }

    @Override
    public Result<Optional<AnswerView>, ApplicationError> handle(GetLatestAnswerQuery query) {
        if (!externalSubscriptionService.hasActivePlan(query.producerId())) {
            return inactivePlan();
        }
        if (!externalOrchardService.existsActivePlot(query.plotId())) {
            return Result.failure(ApplicationError.notFound("plot", query.plotId().plotId()));
        }
        return Result.success(repository.findLatestOfScreen(query.producerId(), query.plotId(),
                        query.screen().questionPrefix(), clock.instant().minus(AssistantAnswer.RETENTION))
                .map(answer -> new AnswerView(answer, factsEvaluator.freshnessOf(answer))));
    }

    private static <T> Result<T, ApplicationError> inactivePlan() {
        return Result.failure(ApplicationError.forbidden("assistant", "assistant.subscription.inactive"));
    }
}
