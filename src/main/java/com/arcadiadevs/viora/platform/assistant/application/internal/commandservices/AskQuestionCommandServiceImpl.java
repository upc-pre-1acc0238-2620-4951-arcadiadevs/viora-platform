package com.arcadiadevs.viora.platform.assistant.application.internal.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AskQuestionCommandService;
import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerTextChooser;
import com.arcadiadevs.viora.platform.assistant.application.internal.queryservices.AnswerFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.application.queryservices.QuestionCatalogQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerComposer;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

/**
 * Answers the closed questions of the assistant. The question must be one the catalog offers enabled for the plot, so
 * the plan, the ownership of the plot and the data it needs are decided in one place for the app and for this service.
 *
 * <p>Deliberately not transactional as a whole: a producer asking twice with the same client request id at once makes
 * the second save fail on the unique key, and the answer already saved is then read in a fresh transaction.</p>
 */
@Service
public class AskQuestionCommandServiceImpl implements AskQuestionCommandService {

    private final QuestionCatalogQueryService catalogQueryService;
    private final AnswerFactsAssembler factsAssembler;
    private final AssistantAnswerRepository repository;
    private final AnswerLocalizer localizer;
    private final AnswerTextChooser textChooser;
    private final Clock clock;
    private final Duration cacheTtl;

    /**
     * Constructs the command service.
     *
     * @param catalogQueryService decides whether the question can be answered for the plot
     * @param factsAssembler      reads the figures of the plot
     * @param repository          the saved answers
     * @param localizer           writes the answer in a language
     * @param textChooser         keeps the text of the model or the template
     * @param clock               the application clock
     * @param cacheTtl            how long a saved answer is reused for the same data
     */
    public AskQuestionCommandServiceImpl(
            QuestionCatalogQueryService catalogQueryService,
            AnswerFactsAssembler factsAssembler,
            AssistantAnswerRepository repository,
            AnswerLocalizer localizer,
            AnswerTextChooser textChooser,
            Clock clock,
            @Value("${viora.assistant.answer-cache-ttl:PT6H}") Duration cacheTtl
    ) {
        this.catalogQueryService = catalogQueryService;
        this.factsAssembler = factsAssembler;
        this.repository = repository;
        this.localizer = localizer;
        this.textChooser = textChooser;
        this.clock = clock;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public Result<AnswerView, ApplicationError> handle(AskQuestionCommand command) {
        var screen = command.questionId().screen();
        var catalog = catalogQueryService.handle(
                new GetQuestionCatalogQuery(command.producerId(), screen, command.plotId()));
        return switch (catalog) {
            case Result.Failure<ScreenQuestionCatalog, ApplicationError> failure -> Result.failure(failure.error());
            case Result.Success<ScreenQuestionCatalog, ApplicationError> success ->
                    answer(command, screen, success.value());
        };
    }

    private Result<AnswerView, ApplicationError> answer(AskQuestionCommand command, ScreenKey screen,
                                                        ScreenQuestionCatalog catalog) {
        var offered = catalog.questions().stream()
                .filter(question -> question.questionId().equals(command.questionId()))
                .findFirst();
        if (offered.isEmpty()) {
            return Result.failure(ApplicationError.validationError("questionId", "assistant.question.invalid"));
        }

        var replay = repository.findByClientRequestId(command.producerId(), command.clientRequestId());
        if (replay.isPresent()) {
            return Result.success(new AnswerView(replay.get(), Freshness.UP_TO_DATE));
        }
        if (!offered.get().enabled()) {
            return unavailable();
        }

        var facts = factsAssembler.assemble(screen, command.plotId());
        var version = AnswerComposer.dataVersionOf(screen, facts);
        var now = clock.instant();
        if (!command.forceRefresh()) {
            var reusable = repository.findReusable(command.producerId(), command.plotId(), command.questionId(),
                    command.language(), version, now.minus(cacheTtl))
                    // An answer that fell back to its template is not reused, so the next ask tries the model again
                    .filter(saved -> !saved.fallback());
            if (reusable.isPresent()) {
                return Result.success(new AnswerView(reusable.get(), Freshness.UP_TO_DATE));
            }
        }

        var draft = AnswerComposer.compose(command.questionId(), facts, command.plotId());
        if (draft.isEmpty()) {
            return unavailable();
        }
        var written = localizer.localize(draft.get(), command.language());
        var question = localizer.resolve(offered.get().label(), command.language().locale());
        var text = textChooser.choose(command.questionId(), question, command.language(), written.text(),
                written.facts());
        var answer = new AssistantAnswer(AnswerId.generate(), command.producerId(), command.plotId(),
                command.questionId(), question, command.language(), version, text.text(), written.facts(),
                written.sources(), written.actions(), command.clientRequestId(), text.provider(), text.model(),
                text.fallback(), text.inputTokens(), text.outputTokens(), now);
        try {
            repository.save(answer);
        } catch (DataIntegrityViolationException e) {
            return repository.findByClientRequestId(command.producerId(), command.clientRequestId())
                    .map(saved -> Result.<AnswerView, ApplicationError>success(
                            new AnswerView(saved, Freshness.UP_TO_DATE)))
                    .orElseThrow(() -> e);
        }
        return Result.success(new AnswerView(answer, Freshness.UP_TO_DATE));
    }

    private static Result<AnswerView, ApplicationError> unavailable() {
        return Result.failure(ApplicationError.conflict("assistant", "assistant.question.unavailable"));
    }
}
