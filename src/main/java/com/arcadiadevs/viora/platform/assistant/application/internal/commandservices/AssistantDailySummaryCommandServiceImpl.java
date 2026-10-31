package com.arcadiadevs.viora.platform.assistant.application.internal.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantAnswerCommandService.AnswerOutcome;
import com.arcadiadevs.viora.platform.assistant.application.commandservices.AssistantDailySummaryCommandService;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerAssessment;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerFactsEvaluator;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerMetrics;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerRenderer;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerWriter;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.application.limits.AssistantRateLimits;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Prepares the daily summary of Inicio ({@code home.daily_summary}). Order: active plan, read limit (the facts are
 * read on every request), availability (the summary rides on {@code home.week_plan}: 409 without active plots), the
 * stored summary of the day with the same facts and language, and only then a new summary, which spends one answer
 * of the producer's limit. The daily job never spends the limits. A summary is "of the day" while it was created
 * since midnight in Lima and its data has not changed; a summary whose data changed is regenerated, never served as
 * current. With a model writer, a template summary (a fallback) is only served for the shorter
 * {@code fallback-cache-ttl}, as the answers.
 */
@Service
public class AssistantDailySummaryCommandServiceImpl implements AssistantDailySummaryCommandService {

    private static final String SPANISH = "es";
    private static final String ENGLISH = "en";
    private static final QuestionId SUMMARY = QuestionId.HOME_DAILY_SUMMARY;

    private final ExternalSubscriptionService externalSubscriptionService;
    private final AnswerFactsEvaluator factsEvaluator;
    private final AnswerRenderer renderer;
    private final AnswerWriter writer;
    private final AssistantAnswerRepository repository;
    private final AssistantRateLimits rateLimits;
    private final AnswerMetrics metrics;
    private final Clock clock;
    private final Duration fallbackCacheTtl;

    /**
     * Constructs the command service.
     *
     * @param externalSubscriptionService tells whether the producer's plan is active
     * @param factsEvaluator              decides availability and builds the facts
     * @param renderer                    writes the texts from the templates
     * @param writer                      writes the summary text: the template, or a model with the template as
     *                                    fallback
     * @param repository                  stores the summaries with the answers
     * @param rateLimits                  the limits of each producer
     * @param metrics                     counts how each summary ends
     * @param clock                       the application clock
     * @param fallbackCacheTtl            how long a template summary is served again when a model writes the texts
     */
    public AssistantDailySummaryCommandServiceImpl(ExternalSubscriptionService externalSubscriptionService,
            AnswerFactsEvaluator factsEvaluator, AnswerRenderer renderer, AnswerWriter writer,
            AssistantAnswerRepository repository, AssistantRateLimits rateLimits, AnswerMetrics metrics, Clock clock,
            @Value("${viora.assistant.fallback-cache-ttl:PT10M}") Duration fallbackCacheTtl) {
        this.externalSubscriptionService = externalSubscriptionService;
        this.factsEvaluator = factsEvaluator;
        this.renderer = renderer;
        this.writer = writer;
        this.repository = repository;
        this.rateLimits = rateLimits;
        this.metrics = metrics;
        this.clock = clock;
        this.fallbackCacheTtl = fallbackCacheTtl;
    }

    @Override
    public Result<AnswerOutcome, ApplicationError> handle(PrepareDailySummaryCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "assistant.command.null"));
        }
        if (!externalSubscriptionService.hasActivePlan(command.producerId())) {
            return Result.failure(ApplicationError.forbidden("assistant", "assistant.subscription.inactive"));
        }
        if (!command.scheduled()) {
            // Every request reads the facades of the other contexts, as a catalog read does
            var wait = rateLimits.tryCatalog(command.producerId());
            if (wait.isPresent()) {
                return limited(wait.getAsLong());
            }
        }
        String language = languageOf(command.locale());
        return factsEvaluator.assess(command.producerId(), SUMMARY, command.plotId())
                .flatMap(assessment -> switch (assessment) {
                    case AnswerAssessment.Unavailable unavailable -> {
                        metrics.answer(SUMMARY, AnswerMetrics.Outcome.UNAVAILABLE);
                        yield Result.failure(ApplicationError.conflict(
                                "assistant_question",
                                renderer.resolve(unavailable.reason(), Locale.forLanguageTag(language)),
                                Map.of("reason", "assistant.question.unavailable")));
                    }
                    case AnswerAssessment.Available available -> summary(command, available, language);
                });
    }

    private Result<AnswerOutcome, ApplicationError> summary(PrepareDailySummaryCommand command,
            AnswerAssessment.Available available, String language) {
        var facts = available.facts();
        var version = facts.dataVersion();
        var now = clock.instant();
        var stored = repository.findLatestCached(command.producerId(), SUMMARY, command.plotId(), language, version,
                        startOfTodayInLima(now))
                .filter(summary -> !isExpiredFallback(summary, now));
        if (stored.isPresent()) {
            metrics.answer(SUMMARY, AnswerMetrics.Outcome.CACHE);
            return Result.success(new AnswerOutcome(new AnswerView(stored.get(), Freshness.UP_TO_DATE), false));
        }
        if (!command.scheduled()) {
            // A new summary may call the model: on demand it is one answer of the producer's limit
            var wait = rateLimits.tryAnswer(command.producerId(), false);
            if (wait.isPresent()) {
                return limited(wait.getAsLong());
            }
        }
        var locale = Locale.forLanguageTag(language);
        var rendered = renderer.render(facts, locale);
        var written = writer.write(new AnswerDraft(SUMMARY, locale, facts.facts(), rendered.text(),
                rendered.actions()));
        // No key comes from the app: each summary gets its own, so the unique key per producer never collides
        var summary = AssistantAnswer.create(UUID.randomUUID(), command.producerId(), command.plotId(), SUMMARY,
                language, version, written.text(), rendered.facts(), rendered.sources(), rendered.actions(),
                written.model(), written.inputTokens(), written.outputTokens(), now);
        var saved = repository.save(summary);
        metrics.answer(SUMMARY, AnswerMetrics.Outcome.CREATED);
        return Result.success(new AnswerOutcome(new AnswerView(saved, Freshness.UP_TO_DATE), true));
    }

    private Result<AnswerOutcome, ApplicationError> limited(long retryAfterSeconds) {
        metrics.answer(SUMMARY, AnswerMetrics.Outcome.RATE_LIMITED);
        return Result.failure(ApplicationError.tooManyRequests("assistant", "assistant.rate_limited",
                retryAfterSeconds));
    }

    /** Just before midnight in Lima, since the repository bound is exclusive: the summaries of today count. */
    private static Instant startOfTodayInLima(Instant now) {
        return LocalDate.ofInstant(now, ProducerTimeZone.ZONE).atStartOfDay(ProducerTimeZone.ZONE).toInstant()
                .minusNanos(1);
    }

    /** A template summary while a model writes the texts, older than the fallback TTL (as the answers). */
    private boolean isExpiredFallback(AssistantAnswer summary, Instant now) {
        return writer.usesModel() && summary.model().fallback()
                && !summary.createdAt().isAfter(now.minus(fallbackCacheTtl));
    }

    /** Spanish, or English for any other language, as the message bundles do. */
    private static String languageOf(Locale locale) {
        return SPANISH.equals(locale.getLanguage()) ? SPANISH : ENGLISH;
    }
}
