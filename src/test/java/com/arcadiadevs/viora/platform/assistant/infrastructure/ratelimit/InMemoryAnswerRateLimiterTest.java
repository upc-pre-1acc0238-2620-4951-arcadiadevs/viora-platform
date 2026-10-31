package com.arcadiadevs.viora.platform.assistant.infrastructure.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryAnswerRateLimiter: new answers and refreshes per producer in a sliding window")
class InMemoryAnswerRateLimiterTest {

    private static final Instant NOW = Instant.parse("2026-10-30T20:00:00Z");

    private final InMemoryAnswerRateLimiter limiter = new InMemoryAnswerRateLimiter(3, 1, 60);

    @Test
    @DisplayName("lets the answers of the window through and then says how long to wait for the oldest to leave")
    void limitsTheAnswers() {
        assertThat(limiter.tryAcquire("p1", false, NOW)).isEmpty();
        assertThat(limiter.tryAcquire("p1", false, NOW.plusSeconds(600))).isEmpty();
        assertThat(limiter.tryAcquire("p1", false, NOW.plusSeconds(1200))).isEmpty();

        assertThat(limiter.tryAcquire("p1", false, NOW.plusSeconds(1800))).isEqualTo(OptionalLong.of(1800));
        assertThat(limiter.tryAcquire("p1", false, NOW.plusSeconds(3600))).isEmpty();
    }

    @Test
    @DisplayName("a refresh counts against both limits and a refused refresh reserves nothing")
    void limitsTheRefreshes() {
        assertThat(limiter.tryAcquire("p1", true, NOW)).isEmpty();

        assertThat(limiter.tryAcquire("p1", true, NOW.plusSeconds(60))).isEqualTo(OptionalLong.of(3540));
        assertThat(limiter.tryAcquire("p1", false, NOW.plusSeconds(60))).isEmpty();
        assertThat(limiter.tryAcquire("p1", false, NOW.plusSeconds(60))).isEmpty();
        assertThat(limiter.tryAcquire("p1", false, NOW.plusSeconds(60))).isPresent();
    }

    @Test
    @DisplayName("each producer has its own limits")
    void perProducer() {
        limiter.tryAcquire("p1", false, NOW);
        limiter.tryAcquire("p1", false, NOW);
        limiter.tryAcquire("p1", false, NOW);

        assertThat(limiter.tryAcquire("p1", false, NOW)).isPresent();
        assertThat(limiter.tryAcquire("p2", false, NOW)).isEmpty();
    }
}
