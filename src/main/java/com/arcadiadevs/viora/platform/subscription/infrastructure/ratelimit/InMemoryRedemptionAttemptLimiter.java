package com.arcadiadevs.viora.platform.subscription.infrastructure.ratelimit;

import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit.RedemptionAttemptLimiter;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
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
 * {@link RedemptionAttemptLimiter} keeping the failed attempts of each producer in memory, in a sliding window.
 * <p>Every attempt is reserved before the code is looked up and given back when it did not fail, so once a producer
 * reaches the maximum of failed attempts inside the window, the next attempt waits until the oldest one leaves the
 * window, however many attempts arrive at the same time. The counts live in this instance only and are lost on restart, which is enough to
 * stop a code from being guessed by trying them all.</p>
 */
@Component
public class InMemoryRedemptionAttemptLimiter implements RedemptionAttemptLimiter {

    private final int maxFailedAttempts;
    private final Duration window;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    /**
     * Creates the limiter.
     *
     * @param maxFailedAttempts the failed attempts allowed inside the window
     * @param windowMinutes     the length of the window in minutes
     * @throws IllegalArgumentException if a setting is not positive
     */
    public InMemoryRedemptionAttemptLimiter(
            @Value("${viora.subscription.redemption.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${viora.subscription.redemption.window-minutes:15}") long windowMinutes
    ) {
        if (maxFailedAttempts < 1 || windowMinutes < 1) {
            throw new IllegalArgumentException("subscription.redemption.limiter.settings.positive");
        }
        this.maxFailedAttempts = maxFailedAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
    }

    @Override
    public OptionalLong tryAcquire(ProducerId producerId, Instant at) {
        var attempts = failures.computeIfAbsent(producerId.producerId(), key -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts, at);
            if (attempts.size() >= maxFailedAttempts) {
                var waitUntil = attempts.peekFirst().plus(window);
                return OptionalLong.of(Math.max(1L, Duration.between(at, waitUntil).toSeconds()));
            }
            attempts.addLast(at);
            return OptionalLong.empty();
        }
    }

    @Override
    public void release(ProducerId producerId, Instant at) {
        var attempts = failures.get(producerId.producerId());
        if (attempts == null) {
            return;
        }
        synchronized (attempts) {
            attempts.removeLastOccurrence(at);
        }
    }

    @Override
    public void reset(ProducerId producerId) {
        failures.remove(producerId.producerId());
    }

    private void prune(Deque<Instant> attempts, Instant at) {
        while (!attempts.isEmpty() && !attempts.peekFirst().plus(window).isAfter(at)) {
            attempts.removeFirst();
        }
    }
}
