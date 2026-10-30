package com.arcadiadevs.viora.platform.assistant.application.internal.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantAnswerCommandService;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerAssessment;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerFactsEvaluator;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerRenderer;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerWriter;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.AskQuestionCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Answers the closed questions. Order: active plan, replay of the same {@code clientRequestId}, availability from
 * the catalog (409 when disabled, before any facts are built), answer cached with the same facts and language, and
 * only then a new answer. Only a new answer calls the writer, so a model is called at most once per answer; the
 * cards, sources and actions always come from the templates.
 */
@Service
public class AssistantAnswerCommandServiceImpl implements AssistantAnswerCommandService {

    private static final String SPANISH = "es";
    private static final String ENGLISH = "en";

    private final ExternalSubscriptionService externalSubscriptionService;
    private final AnswerFactsEvaluator factsEvaluator;
    private final AnswerRenderer renderer;
    private final AnswerWriter writer;
    private final AssistantAnswerRepository repository;
    private final Clock clock;
    private final Duration cacheTtl;

    /**
     * Constructs the command service.
     *
     * @param externalSubscriptionService tells whether the producer's plan is active
     * @param factsEvaluator              decides availability and builds the facts
     * @param renderer                    writes the texts from the templates
     * @param writer                      writes the answer text: the template, or a model with the template as
     *                                    fallback
     * @param repository                  stores the answers
     * @param clock                       the application clock
     * @param cacheTtl                    how long an answer with the same facts is served again
     */
    public AssistantAnswerCommandServiceImpl(ExternalSubscriptionService externalSubscriptionService,
            AnswerFactsEvaluator factsEvaluator, AnswerRenderer renderer, AnswerWriter writer, AssistantAnswerRepository repository,
            Clock clock, @Value("${viora.assistant.cache-ttl:PT6H}") Duration cacheTtl) {
        this.externalSubscriptionService = externalSubscriptionService;
        this.factsEvaluator = factsEvaluator;
        this.renderer = renderer;
        this.writer = writer;
        this.repository = repository;
        this.clock = clock;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public Result<AnswerOutcome, ApplicationError> handle(AskQuestionCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "assistant.command.null"));
        }
        if (!externalSubscriptionService.hasActivePlan(command.producerId())) {
            return Result.failure(ApplicationError.forbidden("assistant", "assistant.subscription.inactive"));
        }
        var replayed = repository.findByClientRequestId(command.producerId(), command.clientRequestId());
        if (replayed.isPresent()) {
            return replay(replayed.get(), command);
        }
        String language = languageOf(command.locale());
        return factsEvaluator.assess(command.producerId(), command.questionId(), command.plotId())
                .flatMap(assessment -> switch (assessment) {
                    case AnswerAssessment.Unavailable unavailable -> Result.failure(ApplicationError.conflict(
                            "assistant_question",
                            renderer.resolve(unavailable.reason(), Locale.forLanguageTag(language)),
                            Map.of("reason", "assistant.question.unavailable")));
                    case AnswerAssessment.Available available -> answer(command, available, language);
                });
    }

    private Result<AnswerOutcome, ApplicationError> answer(AskQuestionCommand command,
            AnswerAssessment.Available available, String language) {
        var facts = available.facts();
        var version = facts.dataVersion();
        var now = clock.instant();
        if (!command.forceRefresh()) {
            var cached = repository.findLatestCached(command.producerId(), command.questionId(), command.plotId(),
                    language, version, now.minus(cacheTtl));
            if (cached.isPresent()) {
                return served(cached.get(), command);
            }
        }
        var locale = Locale.forLanguageTag(language);
        var rendered = renderer.render(facts, locale);
        var written = writer.write(new AnswerDraft(command.questionId(), locale, facts.facts(), rendered.text(),
                rendered.actions()));
        var answer = AssistantAnswer.create(command.clientRequestId(), command.producerId(), command.plotId(),
                command.questionId(), language, version, written.text(), rendered.facts(), rendered.sources(),
                rendered.actions(), written.model(), written.inputTokens(), written.outputTokens(), now);
        try {
            var saved = repository.save(answer);
            return Result.success(new AnswerOutcome(new AnswerView(saved, Freshness.UP_TO_DATE), true));
        } catch (DataIntegrityViolationException exception) {
            // Two requests with the same key raced; the first one stored is the answer of both
            return repository.findByClientRequestId(command.producerId(), command.clientRequestId())
                    .map(stored -> replay(stored, command))
                    .orElseThrow(() -> exception);
        }
    }

    /** Serves a cached answer and binds the request key to it, so a later retry with that key gets it again. */
    private Result<AnswerOutcome, ApplicationError> served(AssistantAnswer cached, AskQuestionCommand command) {
        try {
            repository.bindRequest(command.producerId(), command.clientRequestId(), cached.id());
            return Result.success(new AnswerOutcome(new AnswerView(cached, Freshness.UP_TO_DATE), false));
        } catch (DataIntegrityViolationException exception) {
            // Another request with the same key got there first; its answer is the answer of both
            return repository.findByClientRequestId(command.producerId(), command.clientRequestId())
                    .map(stored -> replay(stored, command))
                    .orElseThrow(() -> exception);
        }
    }

    private Result<AnswerOutcome, ApplicationError> replay(AssistantAnswer stored, AskQuestionCommand command) {
        if (!stored.questionId().equals(command.questionId()) || !stored.plotId().equals(command.plotId())) {
            return Result.failure(ApplicationError.conflict("assistant_request", "assistant.request.reused"));
        }
        // Idempotent replay: the same response as the first request, without building the facts again
        return Result.success(new AnswerOutcome(new AnswerView(stored, Freshness.UP_TO_DATE), false));
    }

    /** Spanish, or English for any other language, as the message bundles do. */
    private static String languageOf(Locale locale) {
        return SPANISH.equals(locale.getLanguage()) ? SPANISH : ENGLISH;
    }
}
