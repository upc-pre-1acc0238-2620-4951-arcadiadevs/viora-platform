package com.arcadiadevs.viora.platform.iam.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

import java.time.Duration;

/**
 * Domain exception thrown when a new verification code is requested before the resend cooldown elapses.
 */
public class EmailVerificationCooldownException extends BusinessRuleException {

    private final Duration remainingCooldown;

    /**
     * Constructs the exception with the time the account still has to wait.
     *
     * @param remainingCooldown time left until a new code can be sent
     */
    public EmailVerificationCooldownException(Duration remainingCooldown) {
        super("user.email_verification.resend_too_soon");
        this.remainingCooldown = remainingCooldown;
    }

    /**
     * Returns the remaining wait rounded up to whole seconds, as used by the {@code Retry-After} header.
     *
     * @return seconds to wait, at least 1
     */
    public long retryAfterSeconds() {
        var seconds = remainingCooldown.toSeconds() + (remainingCooldown.toNanosPart() > 0 ? 1 : 0);
        return Math.max(1, seconds);
    }
}
