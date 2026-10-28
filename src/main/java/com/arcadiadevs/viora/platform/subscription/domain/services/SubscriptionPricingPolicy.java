package com.arcadiadevs.viora.platform.subscription.domain.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.IndividualHectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.Money;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPrice;

/** Calculates individual annual prices using supplied commercial policy. */
public final class SubscriptionPricingPolicy {
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    /** Decimals of the published rate per hectare of an extension. */
    private static final int RATE_SCALE = 6;
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
        return price(quota, MONTHS_PER_YEAR);
    }

    /**
     * Prices the hectares added to a plan in force for the days it has left: hectares times
     * {@link #extensionRatePerHa}, rounded to cents. The app multiplies the same published rate, so both show the
     * same total.
     * @param addedQuota validated hectares to add
     * @param days days left of the plan, from {@link PlanExtensionPolicy#billableDays}
     * @param periodDays days of the whole plan, from {@link PlanExtensionPolicy#periodDays}
     * @return the extension quote
     * @throws IllegalArgumentException if the hectares are missing or the days are not consistent
     */
    public SubscriptionPrice quoteExtension(IndividualHectaresQuota addedQuota, int days, int periodDays) {
        if (addedQuota == null) {
            throw new IllegalArgumentException("subscription.extension.quote.invalid");
        }
        var total = addedQuota.value().multiply(extensionRatePerHa(days, periodDays)).setScale(2, RoundingMode.HALF_UP);
        return split(total);
    }

    /**
     * The tax-included price of one hectare for the days left of a plan: the yearly rate spread over its days.
     * @param days days left of the plan
     * @param periodDays days of the whole plan
     * @return PEN per hectare, with six decimals
     * @throws IllegalArgumentException if the days are not positive or exceed the plan
     */
    public BigDecimal extensionRatePerHa(int days, int periodDays) {
        if (days < 1 || periodDays < 1 || days > periodDays) {
            throw new IllegalArgumentException("subscription.extension.quote.invalid");
        }
        return monthlyRate.multiply(MONTHS_PER_YEAR).multiply(BigDecimal.valueOf(days))
                .divide(BigDecimal.valueOf(periodDays), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private SubscriptionPrice price(IndividualHectaresQuota quota, BigDecimal months) {
        return split(quota.value().multiply(monthlyRate).multiply(months).setScale(2, RoundingMode.HALF_UP));
    }

    private SubscriptionPrice split(BigDecimal total) {
        var base = total.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
        var tax = total.subtract(base);
        return new SubscriptionPrice(new Money(base), new Money(tax), new Money(total));
    }
}
