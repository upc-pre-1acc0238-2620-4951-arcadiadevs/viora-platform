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
 * Deletes the assistant answers older than the retention, and their idempotency keys, once a day at night in Lima.
 *
 * <p>The history only shows the last 10 days, so older answers are already hidden; this job removes them. A missed
 * run (the free instance asleep) only delays the deletion. Disable it with
 * {@code viora.assistant.retention.enabled=false}.</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "viora.assistant.retention.enabled", havingValue = "true", matchIfMissing = true)
public class AssistantRetentionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AssistantRetentionScheduler.class);

    private final AssistantAnswerRepository answers;
    private final Clock clock;
    private final Duration retention;

    /**
     * Creates the job.
     *
     * @param answers       the assistant answers
     * @param clock         the application clock
     * @param retentionDays days an answer is kept; at least the 10 days of the history
     */
    public AssistantRetentionScheduler(AssistantAnswerRepository answers, Clock clock,
            @Value("${viora.assistant.retention.days:10}") long retentionDays) {
        if (retentionDays < 10) {
            throw new IllegalArgumentException("assistant.retention_days.too_short");
        }
        this.answers = answers;
        this.clock = clock;
        this.retention = Duration.ofDays(retentionDays);
    }

    /** Deletes the expired answers and keys; the repository does it in one transaction. */
    @Scheduled(cron = "${viora.assistant.retention.cron:0 30 3 * * *}", zone = "America/Lima")
    public void run() {
        try {
            int deleted = answers.deleteCreatedBefore(clock.instant().minus(retention));
            if (deleted > 0) {
                LOG.info("Deleted {} assistant answers older than {} days", deleted, retention.toDays());
            }
        } catch (RuntimeException exception) {
            LOG.warn("Assistant retention failed: {}", exception.getMessage());
        }
    }
}
