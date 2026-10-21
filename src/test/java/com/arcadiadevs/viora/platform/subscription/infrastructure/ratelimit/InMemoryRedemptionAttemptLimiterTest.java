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
    @DisplayName("The producer waits once the failed attempts reach the maximum, until the oldest leaves the window")
    void slidingWindow() {
        for (var minute = 0; minute < 3; minute++) {
            assertThat(limiter.retryAfterSeconds(producer, START.plus(Duration.ofMinutes(minute)))).isEmpty();
            limiter.recordFailure(producer, START.plus(Duration.ofMinutes(minute)));
        }

        assertThat(limiter.retryAfterSeconds(producer, START.plus(Duration.ofMinutes(5)))).hasValue(600L);
        assertThat(limiter.retryAfterSeconds(producer, START.plus(Duration.ofMinutes(15)))).isEmpty();
        assertThat(limiter.retryAfterSeconds(new ProducerId(), START.plus(Duration.ofMinutes(5)))).isEmpty();
    }

    @Test
    @DisplayName("A redemption forgets the failed attempts")
    void resetForgetsFailures() {
        for (var attempt = 0; attempt < 3; attempt++) {
            limiter.recordFailure(producer, START);
        }

        limiter.reset(producer);

        assertThat(limiter.retryAfterSeconds(producer, START)).isEmpty();
    }

    @Test
    @DisplayName("The settings must be positive")
    void settingsArePositive() {
        assertThatThrownBy(() -> new InMemoryRedemptionAttemptLimiter(0, 15))
                .hasMessage("subscription.redemption.limiter.settings.positive");
    }
}
