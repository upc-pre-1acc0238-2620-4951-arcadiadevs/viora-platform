package com.arcadiadevs.viora.platform.iam.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Domain exception thrown when a submitted email verification code does not match the active one.
 */
public class InvalidEmailVerificationCodeException extends BusinessRuleException {

    private final int remainingAttempts;

    /**
     * Constructs the exception with the attempts left after this wrong code.
     *
     * @param remainingAttempts wrong codes that can still be submitted
     */
    public InvalidEmailVerificationCodeException(int remainingAttempts) {
        super("user.email_verification.invalid_code");
        this.remainingAttempts = remainingAttempts;
    }

    /**
     * Returns the wrong codes that can still be submitted before the code is locked.
     *
     * @return the remaining attempts
     */
    public int remainingAttempts() {
        return remainingAttempts;
    }
}
