package com.arcadiadevs.viora.platform.subscription.domain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
/** Computes an annual server-owned quote without framework or payment-provider dependencies. */
public final class SubscriptionPricingPolicy {
    private final BigDecimal monthlyRate;
    private final BigDecimal taxRate;
    /** @param monthlyRate net PEN per hectare/month @param taxRate fractional tax rate */
    public SubscriptionPricingPolicy(BigDecimal monthlyRate, BigDecimal taxRate) {
        if (monthlyRate == null || monthlyRate.signum() <= 0 || taxRate == null
                || taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("subscription.tariff.invalid");
        }
        this.monthlyRate = monthlyRate;
        this.taxRate = taxRate;
    }
    /** @param quota contracted capacity @return quote in exact cents */
    public SubscriptionPrice quote(HectaresQuota quota) {
        var base = quota.value().multiply(monthlyRate).multiply(new BigDecimal("12"))
                .setScale(2, RoundingMode.HALF_UP);
        var tax = base.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        return new SubscriptionPrice(new Money(base), new Money(tax), new Money(base.add(tax)));
    }
}
