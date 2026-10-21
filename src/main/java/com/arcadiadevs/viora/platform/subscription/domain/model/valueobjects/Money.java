package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Exact nonnegative monetary amount fitting the payment persistence precision.
 * @param amount amount in soles
 */
public record Money(BigDecimal amount) {
    private static final int MAXIMUM_INTEGER_DIGITS = 17;

    /**
     * Rejects fractional cents rather than silently changing a provider amount.
     * @throws IllegalArgumentException if the amount is invalid or cannot be stored
     */
    public Money {
        if (amount == null || amount.signum() < 0
                || amount.precision() - amount.scale() > MAXIMUM_INTEGER_DIGITS) {
            throw new IllegalArgumentException("subscription.money.invalid");
        }
        try {
            amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("subscription.money.invalid", exception);
        }
    }
}
