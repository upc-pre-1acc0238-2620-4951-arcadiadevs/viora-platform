package com.arcadiadevs.viora.platform.subscription.domain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.IndividualHectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.Money;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPrice;

/** Calculates individual annual prices using supplied commercial policy. */
public final class SubscriptionPricingPolicy {
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private final BigDecimal monthlyRate;
    private final BigDecimal taxRate;

    /**
     * Creates the pricing policy without deployment-specific defaults.
     * @param monthlyRate tax-inclusive PEN per hectare per month
     * @param taxRate fractional tax rate
     * @throws IllegalArgumentException if either rate is invalid
     */
    public SubscriptionPricingPolicy(BigDecimal monthlyRate, BigDecimal taxRate) {
        if (monthlyRate == null || monthlyRate.signum() <= 0 || taxRate == null
                || taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("subscription.tariff.invalid");
        }
        this.monthlyRate = monthlyRate;
        this.taxRate = taxRate;
    }

    /**
     * Extracts included tax from the annual total with exact cents, rejecting storage overflow.
     * @param quota validated individual purchase capacity
     * @return the annual quote
     * @throws IllegalArgumentException if capacity is missing or amounts exceed precision
     */
    public SubscriptionPrice quote(IndividualHectaresQuota quota) {
        if (quota == null) {
            throw new IllegalArgumentException("subscription.quota.invalid");
        }
        var total = quota.value().multiply(monthlyRate).multiply(MONTHS_PER_YEAR)
                .setScale(2, RoundingMode.HALF_UP);
        var base = total.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
        var tax = total.subtract(base);
        return new SubscriptionPrice(new Money(base), new Money(tax), new Money(total));
    }
}
