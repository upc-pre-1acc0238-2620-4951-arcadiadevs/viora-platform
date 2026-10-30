package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Stops calling the model after a run of failures, so a provider that is down costs no latency on every question.
 *
 * <p>Closed: every call goes through and consecutive failures are counted. After {@code failureThreshold} of them it
 * opens and refuses calls for {@code openFor}. Then it is half-open: one call goes through as a trial while the
 * others are still refused; a success closes it and a failure opens it again. Only failures of the model count
 * (errors, timeouts, malformed output); a text the validator rejects is a working model. Thread-safe.</p>
 */
public class ModelCircuitBreaker {

    /** State of the breaker. */
    public enum State {
        /** Calls go through. */
        CLOSED,
        /** Calls are refused. */
        OPEN,
        /** One trial call is in flight; the others are refused. */
        HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration openFor;
    private final Clock clock;
    private State state = State.CLOSED;
    private int failures;
    private Instant openedAt;

    /**
     * Creates a closed breaker.
     *
     * @param failureThreshold consecutive failures that open it
     * @param openFor          how long it refuses calls once open
     * @param clock            the clock
     * @throws IllegalArgumentException if a setting is not positive
     */
    public ModelCircuitBreaker(int failureThreshold, Duration openFor, Clock clock) {
        if (failureThreshold < 1 || openFor == null || openFor.isNegative() || openFor.isZero()) {
            throw new IllegalArgumentException("assistant.breaker.settings.positive");
        }
        this.failureThreshold = failureThreshold;
        this.openFor = openFor;
        this.clock = clock;
    }

    /**
     * Asks to call the model. A caller that gets true must report the outcome with {@link #onSuccess()} or
     * {@link #onFailure()}.
     *
     * @return true when the call may go through
     */
    public synchronized boolean tryAcquire() {
        return switch (state) {
            case CLOSED -> true;
            case HALF_OPEN -> false;
            case OPEN -> {
                if (clock.instant().isBefore(openedAt.plus(openFor))) {
                    yield false;
                }
                state = State.HALF_OPEN;
                yield true;
            }
        };
    }

    /** The model answered: the breaker closes and the failures start again from zero. */
    public synchronized void onSuccess() {
        state = State.CLOSED;
        failures = 0;
    }

    /** The model failed: counts the failure, and opens the breaker at the threshold or after a failed trial. */
    public synchronized void onFailure() {
        if (state == State.OPEN) {
            // A call that started before the breaker opened; it is already open
            return;
        }
        failures++;
        if (state == State.HALF_OPEN || failures >= failureThreshold) {
            state = State.OPEN;
            openedAt = clock.instant();
            failures = 0;
        }
    }

    /**
     * The current state, without moving it; an open breaker past its time still reads open until the next call.
     *
     * @return the state
     */
    public synchronized State state() {
        return state;
    }
}
