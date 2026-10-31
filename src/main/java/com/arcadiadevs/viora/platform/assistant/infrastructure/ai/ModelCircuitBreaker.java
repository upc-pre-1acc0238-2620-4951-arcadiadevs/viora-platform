package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Stops calling the language model for a while after it failed several times in a row, so while the provider is down
 * or out of quota every producer gets the template at once instead of waiting for the timeout. After the pause a
 * single call is let through: if it answers the model is used again, if it fails the pause starts over.
 */
@Component
public class ModelCircuitBreaker {

    private final int failureThreshold;
    private final Duration openFor;
    private final Clock clock;

    private int consecutiveFailures;
    private Instant openUntil;
    private boolean trialInFlight;

    /**
     * Creates the breaker.
     *
     * @param failureThreshold failures in a row that pause the model
     * @param openFor          how long the model is paused
     * @param clock            the application clock
     * @throws IllegalArgumentException if a setting is not positive
     */
    public ModelCircuitBreaker(
            @Value("${viora.assistant.model.circuit.failure-threshold:5}") int failureThreshold,
            @Value("${viora.assistant.model.circuit.open-for:PT2M}") Duration openFor,
            Clock clock
    ) {
        if (failureThreshold < 1 || openFor == null || openFor.isNegative() || openFor.isZero()) {
            throw new IllegalArgumentException("assistant.circuit.settings.positive");
        }
        this.failureThreshold = failureThreshold;
        this.openFor = openFor;
        this.clock = clock;
    }

    /**
     * Whether the model may be called now. While paused it answers false; once the pause is over it answers true to
     * one caller only, until that call reports how it went.
     *
     * @return true when the model may be called
     */
    public synchronized boolean tryCall() {
        if (openUntil == null) {
            return true;
        }
        if (clock.instant().isBefore(openUntil) || trialInFlight) {
            return false;
        }
        trialInFlight = true;
        return true;
    }

    /** Reports that the model answered: it is used again by everyone. */
    public synchronized void onSuccess() {
        consecutiveFailures = 0;
        openUntil = null;
        trialInFlight = false;
    }

    /** Reports that the model failed or timed out. */
    public synchronized void onFailure() {
        consecutiveFailures++;
        if (trialInFlight || consecutiveFailures >= failureThreshold) {
            openUntil = clock.instant().plus(openFor);
        }
        trialInFlight = false;
    }

    /**
     * Whether the model is paused now.
     *
     * @return true while calls are refused
     */
    public synchronized boolean isOpen() {
        return openUntil != null && clock.instant().isBefore(openUntil);
    }
}
