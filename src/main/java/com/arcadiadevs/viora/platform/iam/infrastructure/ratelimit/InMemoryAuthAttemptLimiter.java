package com.arcadiadevs.viora.platform.iam.infrastructure.ratelimit;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.ratelimit.AuthAttemptLimiter;
import com.arcadiadevs.viora.platform.shared.infrastructure.ratelimit.SlidingWindowCounter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;

/**
 * {@link AuthAttemptLimiter} keeping the attempts of each email in memory, in sliding windows.
 *
 * <p>The counts live in this instance only and are lost on restart. That is enough on a single instance: an online
 * guess needs thousands of tries, and the window lets through only a handful.</p>
 */
@Component
public class InMemoryAuthAttemptLimiter implements AuthAttemptLimiter {

    private final SlidingWindowCounter signInFailures;
    private final SlidingWindowCounter passwordResetRequests;

    /**
     * Creates the limiter.
     *
     * @param maxSignInFailures       failed sign-ins allowed per email inside the window
     * @param signInWindowMinutes     length of the sign-in window, in minutes
     * @param maxPasswordResets       reset links sent per email inside the window
     * @param passwordResetWindowMinutes length of the reset window, in minutes
     * @throws IllegalArgumentException if a setting is not positive
     */
    public InMemoryAuthAttemptLimiter(
            @Value("${viora.iam.sign-in.max-failed-attempts:5}") int maxSignInFailures,
            @Value("${viora.iam.sign-in.window-minutes:15}") long signInWindowMinutes,
            @Value("${viora.iam.password-reset.max-requests:3}") int maxPasswordResets,
            @Value("${viora.iam.password-reset.window-minutes:60}") long passwordResetWindowMinutes
    ) {
        this.signInFailures = new SlidingWindowCounter(maxSignInFailures, Duration.ofMinutes(signInWindowMinutes));
        this.passwordResetRequests =
                new SlidingWindowCounter(maxPasswordResets, Duration.ofMinutes(passwordResetWindowMinutes));
    }

    @Override
    public OptionalLong tryAcquireSignIn(String email, Instant at) {
        return signInFailures.tryAcquire(email, at);
    }

    @Override
    public void resetSignIn(String email) {
        signInFailures.reset(email);
    }

    @Override
    public boolean tryAcquirePasswordReset(String email, Instant at) {
        return passwordResetRequests.tryAcquire(email, at).isEmpty();
    }
}
