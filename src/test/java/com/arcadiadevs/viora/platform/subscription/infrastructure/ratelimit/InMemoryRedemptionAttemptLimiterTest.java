package com.arcadiadevs.viora.platform.subscription.infrastructure.ratelimit;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("In-memory redemption attempt limiter unit tests")
class InMemoryRedemptionAttemptLimiterTest {

    private static final Instant START = Instant.parse("2026-10-20T15:00:00Z");

    private final InMemoryRedemptionAttemptLimiter limiter = new InMemoryRedemptionAttemptLimiter(3, 15);
    private final ProducerId producer = new ProducerId();

    @Test
    @DisplayName("The producer waits once the attempts reach the maximum, until the oldest leaves the window")
    void slidingWindow() {
        for (var minute = 0; minute < 3; minute++) {
            assertThat(limiter.tryAcquire(producer, START.plus(Duration.ofMinutes(minute)))).isEmpty();
        }

        assertThat(limiter.tryAcquire(producer, START.plus(Duration.ofMinutes(5)))).hasValue(600L);
        assertThat(limiter.tryAcquire(producer, START.plus(Duration.ofMinutes(15)))).isEmpty();
        assertThat(limiter.tryAcquire(new ProducerId(), START.plus(Duration.ofMinutes(5)))).isEmpty();
    }

    @Test
    @DisplayName("A refused attempt is not counted, and a released attempt is given back")
    void refusedAndReleasedAttemptsDoNotCount() {
        for (var attempt = 0; attempt < 3; attempt++) {
            limiter.tryAcquire(producer, START);
        }
        limiter.tryAcquire(producer, START.plus(Duration.ofMinutes(1)));

        limiter.release(producer, START);

        assertThat(limiter.tryAcquire(producer, START.plus(Duration.ofMinutes(2)))).isEmpty();
        assertThat(limiter.tryAcquire(producer, START.plus(Duration.ofMinutes(2)))).isPresent();
    }

    @Test
    @DisplayName("A redemption forgets the attempts")
    void resetForgetsAttempts() {
        for (var attempt = 0; attempt < 3; attempt++) {
            limiter.tryAcquire(producer, START);
        }

        limiter.reset(producer);

        assertThat(limiter.tryAcquire(producer, START)).isEmpty();
    }

    @Test
    @DisplayName("The settings must be positive")
    void settingsArePositive() {
        assertThatThrownBy(() -> new InMemoryRedemptionAttemptLimiter(0, 15))
                .hasMessage("subscription.redemption.limiter.settings.positive");
    }
}
