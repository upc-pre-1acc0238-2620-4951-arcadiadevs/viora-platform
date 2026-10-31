package com.arcadiadevs.viora.platform.assistant.infrastructure.ratelimit;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ratelimit.AnswerRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link AnswerRateLimiter} keeping the answers of each producer in memory, in a sliding window. A refresh counts
 * against both limits, and is reserved in both or in none. The counts live in this instance only and are lost on
 * restart, which is enough to keep one producer from using up the quota of the model.
 */
@Component
public class InMemoryAnswerRateLimiter implements AnswerRateLimiter {

    private final int maxAnswers;
    private final int maxRefreshes;
    private final Duration window;
    private final Map<String, Usage> usages = new ConcurrentHashMap<>();

    /** The answers and the refreshes of one producer inside the window. */
    private static final class Usage {
        private final Deque<Instant> answers = new ArrayDeque<>();
        private final Deque<Instant> refreshes = new ArrayDeque<>();
    }

    /**
     * Creates the limiter.
     *
     * @param maxAnswers    new answers allowed per producer inside the window
     * @param maxRefreshes  of those, how many may ignore the saved answer
     * @param windowMinutes the length of the window in minutes
     * @throws IllegalArgumentException if a setting is not positive
     */
    public InMemoryAnswerRateLimiter(
            @Value("${viora.assistant.limits.answers-per-window:20}") int maxAnswers,
            @Value("${viora.assistant.limits.refreshes-per-window:5}") int maxRefreshes,
            @Value("${viora.assistant.limits.window-minutes:60}") long windowMinutes
    ) {
        if (maxAnswers < 1 || maxRefreshes < 1 || windowMinutes < 1) {
            throw new IllegalArgumentException("assistant.limiter.settings.positive");
        }
        this.maxAnswers = maxAnswers;
        this.maxRefreshes = maxRefreshes;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    @Override
    public OptionalLong tryAcquire(String producerId, boolean forceRefresh, Instant at) {
        // Producers that stopped asking are dropped, so the map cannot grow with producers never seen again
        usages.values().removeIf(usage -> {
            synchronized (usage) {
                prune(usage.answers, at);
                prune(usage.refreshes, at);
                return usage.answers.isEmpty() && usage.refreshes.isEmpty();
            }
        });
        var usage = usages.computeIfAbsent(producerId, key -> new Usage());
        synchronized (usage) {
            prune(usage.answers, at);
            prune(usage.refreshes, at);
            long wait = Math.max(waitFor(usage.answers, maxAnswers, at),
                    forceRefresh ? waitFor(usage.refreshes, maxRefreshes, at) : 0L);
            if (wait > 0) {
                return OptionalLong.of(wait);
            }
            usage.answers.addLast(at);
            if (forceRefresh) {
                usage.refreshes.addLast(at);
            }
            // Put back in case a concurrent sweep removed it while it was empty
            usages.putIfAbsent(producerId, usage);
            return OptionalLong.empty();
        }
    }

    /** Seconds until the oldest event leaves the window when the limit is reached, 0 otherwise. */
    private long waitFor(Deque<Instant> events, int max, Instant at) {
        if (events.size() < max) {
            return 0L;
        }
        return Math.max(1L, Duration.between(at, events.peekFirst().plus(window)).toSeconds());
    }

    private void prune(Deque<Instant> events, Instant at) {
        while (!events.isEmpty() && !events.peekFirst().plus(window).isAfter(at)) {
            events.removeFirst();
        }
    }
}
