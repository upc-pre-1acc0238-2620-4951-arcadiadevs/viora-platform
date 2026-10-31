package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.application.queryservices.AssistantAnswerQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerByIdQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetAnswerHistoryQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetLatestAnswerQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerComposer;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the answers the assistant already gave. The history keeps ten days; each answer comes with whether the data
 * of its plot is still the one it was written from, which is computed now and never stored.
 */
@Service
public class AssistantAnswerQueryServiceImpl implements AssistantAnswerQueryService {

    private final ExternalSubscriptionService externalSubscriptionService;
    private final AssistantAnswerRepository repository;
    private final AnswerFactsAssembler factsAssembler;
    private final Clock clock;
    private final Duration historyWindow;

    /**
     * Constructs the query service.
     *
     * @param externalSubscriptionService tells whether the producer's plan is active
     * @param repository                  the saved answers
     * @param factsAssembler              reads the current figures of the plot, to tell if an answer is stale
     * @param clock                       the application clock
     * @param historyDays                 how many days an answer stays in the history
     */
    public AssistantAnswerQueryServiceImpl(
            ExternalSubscriptionService externalSubscriptionService,
            AssistantAnswerRepository repository,
            AnswerFactsAssembler factsAssembler,
            Clock clock,
            @Value("${viora.assistant.history-days:10}") int historyDays
    ) {
        this.externalSubscriptionService = externalSubscriptionService;
        this.repository = repository;
        this.factsAssembler = factsAssembler;
        this.clock = clock;
        this.historyWindow = Duration.ofDays(historyDays);
    }

    @Override
    public Result<List<AnswerView>, ApplicationError> handle(GetAnswerHistoryQuery query) {
        if (!externalSubscriptionService.hasActivePlan(query.producerId())) {
            return Result.failure(inactivePlan());
        }
        var answers = repository.findRecent(query.producerId(), since(), query.limit());
        var versions = new HashMap<String, DataVersion>();
        return Result.success(answers.stream().map(answer -> view(answer, versions)).toList());
    }

    @Override
    public Result<AnswerView, ApplicationError> handle(GetAnswerByIdQuery query) {
        if (!externalSubscriptionService.hasActivePlan(query.producerId())) {
            return Result.failure(inactivePlan());
        }
        return repository.findByIdAndProducerId(query.answerId(), query.producerId())
                .filter(answer -> !answer.createdAt().isBefore(since()))
                .<Result<AnswerView, ApplicationError>>map(answer -> Result.success(view(answer, new HashMap<>())))
                .orElseGet(() -> Result.failure(ApplicationError.notFound("Answer", query.answerId().value())));
    }

    @Override
    public Result<AnswerView, ApplicationError> handle(GetLatestAnswerQuery query) {
        if (!externalSubscriptionService.hasActivePlan(query.producerId())) {
            return Result.failure(inactivePlan());
        }
        var prefix = query.screen().questionPrefix();
        return repository.findLatestOfScreen(query.producerId(), query.plotId(), prefix, since())
                .<Result<AnswerView, ApplicationError>>map(answer -> Result.success(view(answer, new HashMap<>())))
                .orElseGet(() -> Result.failure(ApplicationError.notFound("Answer", "assistant.answer.none")));
    }

    private Instant since() {
        return clock.instant().minus(historyWindow);
    }

    /** Several answers of one screen and plot share one read of the figures. */
    private AnswerView view(AssistantAnswer answer, Map<String, DataVersion> versions) {
        var screen = answer.questionId().screen();
        var current = versions.computeIfAbsent(screen + "|" + answer.plotId().plotId(),
                key -> AnswerComposer.dataVersionOf(screen, factsAssembler.assemble(screen, answer.plotId())));
        return new AnswerView(answer, answer.freshness(current));
    }

    private static ApplicationError inactivePlan() {
        return ApplicationError.forbidden("assistant", "assistant.subscription.inactive");
    }
}
