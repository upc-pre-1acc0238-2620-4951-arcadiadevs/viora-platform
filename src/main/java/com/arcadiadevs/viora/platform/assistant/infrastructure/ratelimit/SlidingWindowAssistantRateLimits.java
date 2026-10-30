package com.arcadiadevs.viora.platform.assistant.infrastructure.ratelimit;

import com.arcadiadevs.viora.platform.assistant.application.limits.AssistantRateLimits;
import com.arcadiadevs.viora.platform.shared.infrastructure.ratelimit.SlidingWindowCounter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.OptionalLong;

/**
 * The assistant limits per producer, in memory, over a sliding hour ({@link SlidingWindowCounter}). Like the other
 * limits of the platform they live in one instance and start empty after a restart.
 *
 * <p>A refresh is checked before the answers limit, so a refused refresh does not spend an answer; a refresh allowed
 * and then refused by the answers limit spends its refresh slot, since the counter cannot give one back.</p>
 */
@Component
public class SlidingWindowAssistantRateLimits implements AssistantRateLimits {

    private static final Duration HOUR = Duration.ofHours(1);

    private final SlidingWindowCounter answers;
    private final SlidingWindowCounter refreshes;
    private final SlidingWindowCounter catalog;
    private final Clock clock;

    /**
     * Creates the limits.
     *
     * @param answersPerHour   questions per producer and hour, replays excluded
     * @param refreshesPerHour questions with {@code forceRefresh} per producer and hour
     * @param catalogPerHour   catalog reads per producer and hour
     * @param clock            the application clock
     */
    public SlidingWindowAssistantRateLimits(
            @Value("${viora.assistant.rate-limit.answers-per-hour:20}") int answersPerHour,
            @Value("${viora.assistant.rate-limit.refreshes-per-hour:5}") int refreshesPerHour,
            @Value("${viora.assistant.rate-limit.catalog-per-hour:60}") int catalogPerHour,
            Clock clock) {
        this.answers = new SlidingWindowCounter(answersPerHour, HOUR);
        this.refreshes = new SlidingWindowCounter(refreshesPerHour, HOUR);
        this.catalog = new SlidingWindowCounter(catalogPerHour, HOUR);
        this.clock = clock;
    }

    @Override
    public OptionalLong tryAnswer(String producerId, boolean forceRefresh) {
        var now = clock.instant();
        if (forceRefresh) {
            var wait = refreshes.tryAcquire(producerId, now);
            if (wait.isPresent()) {
                return wait;
            }
        }
        return answers.tryAcquire(producerId, now);
    }

    @Override
    public OptionalLong tryCatalog(String producerId) {
        return catalog.tryAcquire(producerId, clock.instant());
    }
}
