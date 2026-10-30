package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.MutableClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ModelCircuitBreaker: opens after consecutive failures, one trial when half-open")
class ModelCircuitBreakerTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-30T15:00:00Z"));
    private final ModelCircuitBreaker breaker = new ModelCircuitBreaker(3, Duration.ofMinutes(2), clock);

    @Test
    @DisplayName("Stays closed below the threshold; a success resets the count")
    void successResetsTheCount() {
        fail(2);
        breaker.onSuccess();
        fail(2);

        assertThat(breaker.state()).isEqualTo(ModelCircuitBreaker.State.CLOSED);
        assertThat(breaker.tryAcquire()).isTrue();
    }

    @Test
    @DisplayName("Opens at the threshold and refuses calls until its time passes")
    void opensAtTheThreshold() {
        fail(3);

        assertThat(breaker.state()).isEqualTo(ModelCircuitBreaker.State.OPEN);
        assertThat(breaker.tryAcquire()).isFalse();
        clock.advance(Duration.ofMinutes(2).minusSeconds(1));
        assertThat(breaker.tryAcquire()).isFalse();
    }

    @Test
    @DisplayName("Half-open lets one trial through; its failure opens it again for the whole time")
    void failedTrialOpensAgain() {
        fail(3);
        clock.advance(Duration.ofMinutes(2));

        assertThat(breaker.tryAcquire()).isTrue();
        assertThat(breaker.tryAcquire()).isFalse();
        breaker.onFailure();

        assertThat(breaker.state()).isEqualTo(ModelCircuitBreaker.State.OPEN);
        clock.advance(Duration.ofMinutes(1));
        assertThat(breaker.tryAcquire()).isFalse();
        clock.advance(Duration.ofMinutes(1));
        assertThat(breaker.tryAcquire()).isTrue();
    }

    @Test
    @DisplayName("A successful trial closes it")
    void successfulTrialCloses() {
        fail(3);
        clock.advance(Duration.ofMinutes(2));
        breaker.tryAcquire();

        breaker.onSuccess();

        assertThat(breaker.state()).isEqualTo(ModelCircuitBreaker.State.CLOSED);
        assertThat(breaker.tryAcquire()).isTrue();
        assertThat(breaker.tryAcquire()).isTrue();
    }

    @Test
    @DisplayName("Many threads at the end of the open time get exactly one trial call")
    void oneTrialAcrossThreads() throws Exception {
        fail(3);
        clock.advance(Duration.ofMinutes(2));
        var start = new CountDownLatch(1);
        var granted = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 32; i++) {
                pool.submit(() -> {
                    start.await();
                    if (breaker.tryAcquire()) {
                        granted.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(granted).hasValue(1);
    }

    @Test
    @DisplayName("Rejects settings that are not positive")
    void rejectsBadSettings() {
        assertThatThrownBy(() -> new ModelCircuitBreaker(0, Duration.ofMinutes(1), clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModelCircuitBreaker(1, Duration.ZERO, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            assertThat(breaker.tryAcquire()).isTrue();
            breaker.onFailure();
        }
    }
}
