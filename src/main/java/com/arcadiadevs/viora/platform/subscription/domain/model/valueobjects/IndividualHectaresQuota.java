package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.math.BigDecimal;

/**
 * Capacity requested for an individual purchase, independent of cooperative license balances.
 * @param value hectares in half-hectare increments
 */
public record IndividualHectaresQuota(BigDecimal value) {
    private static final BigDecimal MINIMUM = new BigDecimal("0.5");
    private static final BigDecimal INCREMENT = new BigDecimal("0.5");
    private static final int MAXIMUM_INTEGER_DIGITS = 18;

    /**
     * Validates purchase capacity and the storage precision boundary.
     * @throws IllegalArgumentException if capacity is invalid or cannot be stored
     */
    public IndividualHectaresQuota {
        if (value == null || value.compareTo(MINIMUM) < 0
                || value.remainder(INCREMENT).signum() != 0
                || value.precision() - value.scale() > MAXIMUM_INTEGER_DIGITS) {
            throw new IllegalArgumentException("subscription.quota.invalid");
        }
        value = value.setScale(1);
    }
}
