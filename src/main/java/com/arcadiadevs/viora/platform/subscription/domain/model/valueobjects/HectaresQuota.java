package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object holding a surface in hectares covered by a subscription or a license, with two decimals.
 *
 * @param hectares the surface in hectares; at least 0.1 and below one million
 */
public record HectaresQuota(BigDecimal hectares) {

    private static final BigDecimal MINIMUM = new BigDecimal("0.10");
    private static final BigDecimal LIMIT = new BigDecimal("1000000");

    /**
     * Validates the surface and rounds it to two decimals.
     *
     * @throws IllegalArgumentException if it is null, below 0.1 ha, or one million hectares or more
     */
    public HectaresQuota {
        if (hectares == null) {
            throw new IllegalArgumentException("subscription.quota.null");
        }
        hectares = hectares.setScale(2, RoundingMode.HALF_UP);
        if (hectares.compareTo(MINIMUM) < 0) {
            throw new IllegalArgumentException("subscription.quota.below_minimum");
        }
        if (hectares.compareTo(LIMIT) >= 0) {
            throw new IllegalArgumentException("subscription.quota.too_large");
        }
    }
}
