package com.arcadiadevs.viora.platform.assistant.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.DailySummaryCommandService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.domain.model.commands.PrepareDailySummaryCommand;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * Precomputes the daily summary of the home screen once a day in the morning in Lima (06:30): the summary of the
 * whole field, in Spanish, of every producer with active plots. A plot summary, or one in English, is written on
 * demand by {@code GET /assistant/daily-summary}. The job never spends the producers' limits.
 *
 * <p>One producer failing (a facade down, the model failing) never stops the rest: it is counted and logged with the
 * exception type only, never an identifier or a message that could carry one. A producer without an active plan, or
 * whose summary of the day is already current, is skipped. The run is bounded by
 * {@code viora.assistant.daily-summary.max-producers}; producers past it get their summary on demand. A missed run (the
 * instance asleep) only means the first request of the day writes it. Disable it with
 * {@code viora.assistant.daily-summary.enabled=false}.</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "viora.assistant.daily-summary.enabled", havingValue = "true", matchIfMissing = true)
public class AssistantDailySummaryScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantDailySummaryScheduler.class);

    private final ExternalOrchardService externalOrchardService;
    private final DailySummaryCommandService summaries;
    private final int maxProducers;

    /**
     * Outcome of one run.
     *
     * @param created summaries written
     * @param current producers whose summary of the day was already current
     * @param skipped producers refused (no active plan, no active plots)
     * @param failed  producers whose summary failed
     */
    public record Run(int created, int current, int skipped, int failed) {
    }

    /**
     * Creates the job.
     *
     * @param externalOrchardService finds the producers with active plots
     * @param summaries              prepares each summary
     * @param maxProducers           most producers prepared in one run
     */
    public AssistantDailySummaryScheduler(ExternalOrchardService externalOrchardService,
            DailySummaryCommandService summaries,
            @Value("${viora.assistant.daily-summary.max-producers:1000}") int maxProducers) {
        if (maxProducers < 1) {
            throw new IllegalArgumentException("assistant.daily_summary.max_producers");
        }
        this.externalOrchardService = externalOrchardService;
        this.summaries = summaries;
        this.maxProducers = maxProducers;
    }

    /** Runs the precomputation at the configured time in Lima. */
    @Scheduled(cron = "${viora.assistant.daily-summary.cron:0 30 6 * * *}", zone = "America/Lima")
    public void run() {
        precompute();
    }

    /**
     * Prepares the field summary of every producer with active plots, one after the other.
     *
     * @return how the run ended
     */
    public Run precompute() {
        final List<String> producers;
        try {
            producers = externalOrchardService.findProducersWithActivePlots();
        } catch (RuntimeException exception) {
            LOG.warn("Assistant daily summaries not prepared: {}", exception.getClass().getSimpleName());
            return new Run(0, 0, 0, 0);
        }
        int created = 0;
        int current = 0;
        int skipped = 0;
        int failed = 0;
        for (var producerId : producers.stream().limit(maxProducers).toList()) {
            try {
                var outcome = summaries.handle(
                        new PrepareDailySummaryCommand(producerId, null, AnswerLanguage.SPANISH, true)).success();
                if (outcome.isEmpty()) {
                    skipped++;
                } else if (outcome.get().created()) {
                    created++;
                } else {
                    current++;
                }
            } catch (RuntimeException exception) {
                failed++;
                LOG.warn("Assistant daily summary of a producer failed: {}", exception.getClass().getSimpleName());
            }
        }
        if (producers.size() > maxProducers) {
            LOG.info("Assistant daily summaries stopped at {} of {} producers", maxProducers, producers.size());
        }
        LOG.info("Assistant daily summaries: {} created, {} current, {} skipped, {} failed", created, current,
                skipped, failed);
        return new Run(created, current, skipped, failed);
    }
}
