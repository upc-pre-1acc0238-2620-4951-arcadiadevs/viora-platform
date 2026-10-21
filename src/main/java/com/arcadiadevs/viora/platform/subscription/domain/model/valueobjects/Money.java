package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
/** Nonnegative PEN amount with exact cents. @param amount amount in soles */
public record Money(BigDecimal amount) {
    public Money {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("subscription.money.invalid");
        }
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
