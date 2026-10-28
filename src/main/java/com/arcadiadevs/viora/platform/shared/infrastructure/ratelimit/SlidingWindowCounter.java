package com.arcadiadevs.viora.platform.shared.infrastructure.ratelimit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts the events of each key inside a sliding window and refuses a new one once the key reaches the maximum.
 *
 * <p>Keys whose window has emptied are dropped on the next acquire of any key, so the map cannot grow without bound
 * with keys that are never seen again.</p>
 */
public final class SlidingWindowCounter {

    private final int maxEvents;
    private final Duration window;
    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();

    /**
     * Creates the counter.
     *
     * @param maxEvents the events allowed per key inside the window
     * @param window    the length of the window
     * @throws IllegalArgumentException if a setting is not positive
     */
    public SlidingWindowCounter(int maxEvents, Duration window) {
        if (maxEvents < 1 || window == null || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("rate_limit.settings.positive");
        }
        this.maxEvents = maxEvents;
        this.window = window;
    }

    /**
     * Records an event for a key unless the key already reached the maximum inside the window.
     *
     * @param key the key the event belongs to
     * @param at  the instant of the event
     * @return empty when the event was recorded, or the seconds until the oldest event leaves the window
     */
    public OptionalLong tryAcquire(String key, Instant at) {
        events.values().removeIf(deque -> {
            synchronized (deque) {
                prune(deque, at);
                return deque.isEmpty();
            }
        });
        var deque = events.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            prune(deque, at);
            if (deque.size() >= maxEvents) {
                var waitUntil = deque.peekFirst().plus(window);
                return OptionalLong.of(Math.max(1L, Duration.between(at, waitUntil).toSeconds()));
            }
            deque.addLast(at);
            // Put back in case a concurrent sweep removed it while it was empty
            events.putIfAbsent(key, deque);
            return OptionalLong.empty();
        }
    }

    /**
     * Forgets every event of a key.
     *
     * @param key the key to forget
     */
    public void reset(String key) {
        events.remove(key);
    }

    private void prune(Deque<Instant> deque, Instant at) {
        while (!deque.isEmpty() && !deque.peekFirst().plus(window).isAfter(at)) {
            deque.removeFirst();
        }
    }
}
