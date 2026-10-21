package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.math.BigDecimal;
/** Contracted area in hectares, in half-hectare increments. @param value requested capacity */
public record HectaresQuota(BigDecimal value) {
    public HectaresQuota {
        if (value == null || value.compareTo(new BigDecimal("0.5")) < 0
                || value.remainder(new BigDecimal("0.5")).signum() != 0) {
            throw new IllegalArgumentException("subscription.quota.invalid");
        }
        value = value.setScale(1);
    }
}
