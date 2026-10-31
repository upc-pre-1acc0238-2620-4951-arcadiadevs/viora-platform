package com.arcadiadevs.viora.platform.assistant.application.internal.commandservices;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.DailySummaryCommandService;
import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerTextChooser;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ratelimit.AnswerRateLimiter;
import com.arcadiadevs.viora.platform.assistant.application.internal.queryservices.AnswerFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.DailySummary;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DailySummaryId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.repositories.DailySummaryRepository;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerComposer;
import com.arcadiadevs.viora.platform.assistant.domain.services.DailySummaryTexts;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Prepares the daily summary of the home screen ({@code home.daily_summary}) with the controls of the answers: an
 * active plan, the plot verified as the catalog does, the figures read by the HOME reader (one plot or the whole
 * field), the text chosen between the model and the template by the same validator, and the producer's answer limit.
 *
 * <p>Order: plan, plot, figures, availability (the summary rides on {@code home.week_plan}: 409 without active
 * plots), the stored summary of the day with the same figures and language, and only then a new summary, which on
 * demand spends one answer of the producer's limit; the daily job never spends it. A summary is "of the day" while it
 * was written since midnight in Lima and its figures have not changed; one whose figures changed is written again,
 * never served as current. As with the answers, a summary that fell back to its template is not served again, so the
 * next request tries the model again.</p>
 */
@Service
public class DailySummaryCommandServiceImpl implements DailySummaryCommandService {

    private static final QuestionId SUMMARY = QuestionId.HOME_DAILY_SUMMARY;
    private static final CatalogText QUESTION = CatalogText.of("assistant.question.home.daily_summary.label");

    private final ExternalSubscriptionService externalSubscriptionService;
    private final ExternalOrchardService externalOrchardService;
    private final AnswerFactsAssembler factsAssembler;
    private final DailySummaryRepository repository;
    private final AnswerLocalizer localizer;
    private final AnswerTextChooser textChooser;
    private final AnswerRateLimiter rateLimiter;
    private final Clock clock;

    /**
     * Constructs the command service.
     *
     * @param externalSubscriptionService tells whether the producer's plan is active
     * @param externalOrchardService      verifies the plot
     * @param factsAssembler              reads the figures of the home screen
     * @param repository                  the stored summaries
     * @param localizer                   writes the summary in a language
     * @param textChooser                 keeps the text of the model or the template
     * @param rateLimiter                 limits the new answers of a producer
     * @param clock                       the application clock
     */
    public DailySummaryCommandServiceImpl(ExternalSubscriptionService externalSubscriptionService,
                                          ExternalOrchardService externalOrchardService,
                                          AnswerFactsAssembler factsAssembler,
                                          DailySummaryRepository repository,
                                          AnswerLocalizer localizer,
                                          AnswerTextChooser textChooser,
                                          AnswerRateLimiter rateLimiter,
                                          Clock clock) {
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalOrchardService = externalOrchardService;
        this.factsAssembler = factsAssembler;
        this.repository = repository;
        this.localizer = localizer;
        this.textChooser = textChooser;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Override
    public Result<Outcome, ApplicationError> handle(PrepareDailySummaryCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "assistant.command.null"));
        }
        if (!externalSubscriptionService.hasActivePlan(command.producerId())) {
            return Result.failure(ApplicationError.forbidden("assistant", "assistant.subscription.inactive"));
        }
        // Without plot (the producer's whole field) there is no plot to verify
        if (command.plotId() != null && !externalOrchardService.existsActivePlot(command.plotId())) {
            return Result.failure(ApplicationError.notFound("plot", command.plotId().plotId()));
        }
        if (!(factsAssembler.assemble(ScreenKey.HOME, command.producerId(), command.plotId())
                instanceof HomeFacts facts)) {
            throw new IllegalStateException("The HOME reader returned the figures of another screen");
        }
        var draft = DailySummaryTexts.summarize(facts);
        if (draft.isEmpty()) {
            return Result.failure(ApplicationError.conflict("assistant", "assistant.question.unavailable"));
        }
        var version = AnswerComposer.dataVersionOf(ScreenKey.HOME, facts);
        var now = clock.instant();
        var stored = repository.findCurrent(command.producerId(), command.plotId(), command.language(), version,
                        startOfTodayInLima(now))
                // A summary that fell back to its template is not served again, so the next request tries the model
                .filter(summary -> !summary.fallback());
        if (stored.isPresent()) {
            return Result.success(new Outcome(stored.get(), false));
        }
        if (!command.scheduled()) {
            // A new summary may call the model: on demand it is one answer of the producer's limit
            var retryAfter = rateLimiter.tryAcquire(command.producerId(), false, now);
            if (retryAfter.isPresent()) {
                return Result.failure(ApplicationError.tooManyRequests("assistant", "assistant.answers.too_many",
                        retryAfter.getAsLong()));
            }
        }
        var written = localizer.localize(draft.get(), command.language());
        var question = localizer.resolve(QUESTION, command.language().locale());
        var text = textChooser.choose(SUMMARY, question, command.language(), written.text(), written.facts());
        var summary = new DailySummary(DailySummaryId.generate(), command.producerId(), command.plotId(),
                command.language(), version, text.text(), written.facts(), written.sources(), text.provider(),
                text.model(), text.fallback(), text.inputTokens(), text.outputTokens(), now);
        repository.save(summary);
        return Result.success(new Outcome(summary, true));
    }

    /** Midnight of today in Lima: the summaries written since then are of the day. */
    private static Instant startOfTodayInLima(Instant now) {
        return LocalDate.ofInstant(now, ProducerTimeZone.ZONE).atStartOfDay(ProducerTimeZone.ZONE).toInstant();
    }
}
