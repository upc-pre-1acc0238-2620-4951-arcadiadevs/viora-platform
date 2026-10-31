package com.arcadiadevs.viora.platform.assistant.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.assistant.domain.repositories.DailySummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;

/**
 * Deletes, once a day, the daily summaries older than the history ({@code viora.assistant.history-days}, 10 by
 * default), as the answers are: only the summary of the day is ever served, and deleting the old ones keeps the
 * figures of a plot from staying in the table for good. Disabled with the answers' retention
 * ({@code viora.assistant.retention.enabled=false}).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "viora.assistant.retention.enabled", havingValue = "true", matchIfMissing = true)
public class AssistantDailySummaryRetentionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantDailySummaryRetentionScheduler.class);

    private final DailySummaryRepository repository;
    private final Clock clock;
    private final Duration retention;

    /**
     * Creates the scheduler.
     *
     * @param repository  the stored summaries
     * @param clock       the application clock
     * @param historyDays days a summary is kept
     */
    public AssistantDailySummaryRetentionScheduler(DailySummaryRepository repository, Clock clock,
            @Value("${viora.assistant.history-days:10}") long historyDays) {
        if (historyDays < 1) {
            throw new IllegalArgumentException("assistant.history_days.positive");
        }
        this.repository = repository;
        this.clock = clock;
        this.retention = Duration.ofDays(historyDays);
    }

    /** Deletes the summaries older than the history. */
    @Scheduled(initialDelayString = "PT20M", fixedDelayString = "PT24H")
    public void run() {
        try {
            int deleted = repository.deleteCreatedBefore(clock.instant().minus(retention));
            if (deleted > 0) {
                LOG.info("Deleted {} assistant daily summaries older than {} days", deleted, retention.toDays());
            }
        } catch (RuntimeException exception) {
            LOG.warn("Assistant daily summary retention failed: {}", exception.getClass().getSimpleName());
        }
    }
}
