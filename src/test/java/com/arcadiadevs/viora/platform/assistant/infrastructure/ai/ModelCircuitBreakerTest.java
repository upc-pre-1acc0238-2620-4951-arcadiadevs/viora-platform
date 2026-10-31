package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelCircuitBreaker: pauses the model after failures in a row")
class ModelCircuitBreakerTest {

    /** A clock the test moves forward. */
    private static final class MovingClock extends Clock {
        private Instant now = Instant.parse("2026-10-30T20:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MovingClock clock = new MovingClock();
    private final ModelCircuitBreaker breaker = new ModelCircuitBreaker(3, Duration.ofMinutes(2), clock);

    @Test
    @DisplayName("stays closed below the threshold, and a success forgets the failures")
    void closedBelowTheThreshold() {
        breaker.onFailure();
        breaker.onFailure();
        breaker.onSuccess();
        breaker.onFailure();
        breaker.onFailure();

        assertThat(breaker.tryCall()).isTrue();
        assertThat(breaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("opens at the threshold and refuses calls until the pause is over")
    void opensAtTheThreshold() {
        breaker.onFailure();
        breaker.onFailure();
        breaker.onFailure();

        assertThat(breaker.isOpen()).isTrue();
        assertThat(breaker.tryCall()).isFalse();
        clock.advance(Duration.ofSeconds(119));
        assertThat(breaker.tryCall()).isFalse();
    }

    @Test
    @DisplayName("after the pause one trial call goes through; its success closes it")
    void trialSuccessCloses() {
        breaker.onFailure();
        breaker.onFailure();
        breaker.onFailure();
        clock.advance(Duration.ofMinutes(2));

        assertThat(breaker.tryCall()).isTrue();
        assertThat(breaker.tryCall()).isFalse();
        breaker.onSuccess();
        assertThat(breaker.tryCall()).isTrue();
        assertThat(breaker.tryCall()).isTrue();
    }

    @Test
    @DisplayName("a failed trial call starts the pause over")
    void trialFailureReopens() {
        breaker.onFailure();
        breaker.onFailure();
        breaker.onFailure();
        clock.advance(Duration.ofMinutes(2));

        assertThat(breaker.tryCall()).isTrue();
        breaker.onFailure();

        assertThat(breaker.isOpen()).isTrue();
        clock.advance(Duration.ofMinutes(1));
        assertThat(breaker.tryCall()).isFalse();
    }
}
