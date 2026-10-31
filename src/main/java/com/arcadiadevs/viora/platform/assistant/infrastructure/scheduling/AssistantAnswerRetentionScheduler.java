package com.arcadiadevs.viora.platform.assistant.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
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
 * Deletes, once a day, the answers older than the history ({@code viora.assistant.history-days}, 10 by default). The
 * history already hides them; deleting them keeps the figures of a plot from staying in the table for good. Disable it
 * with {@code viora.assistant.retention.enabled=false}.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "viora.assistant.retention.enabled", havingValue = "true", matchIfMissing = true)
public class AssistantAnswerRetentionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantAnswerRetentionScheduler.class);

    private final AssistantAnswerRepository repository;
    private final Clock clock;
    private final Duration retention;

    /**
     * Creates the scheduler.
     *
     * @param repository  the saved answers
     * @param clock       the application clock
     * @param historyDays days an answer stays in the history
     */
    public AssistantAnswerRetentionScheduler(AssistantAnswerRepository repository, Clock clock,
            @Value("${viora.assistant.history-days:10}") long historyDays) {
        if (historyDays < 1) {
            throw new IllegalArgumentException("assistant.history_days.positive");
        }
        this.repository = repository;
        this.clock = clock;
        this.retention = Duration.ofDays(historyDays);
    }

    /** Deletes the answers that left the history. */
    @Scheduled(initialDelayString = "PT15M", fixedDelayString = "PT24H")
    public void run() {
        try {
            int deleted = repository.deleteCreatedBefore(clock.instant().minus(retention));
            if (deleted > 0) {
                LOG.info("Deleted {} assistant answers older than {} days", deleted, retention.toDays());
            }
        } catch (RuntimeException exception) {
            LOG.warn("Assistant answer retention failed: {}", exception.getClass().getSimpleName());
        }
    }
}
