package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Weight of delivered olives in kilograms; finite and not negative.
 *
 * @param kilograms weight in kilograms
 */
public record OliveWeight(Double kilograms) {

    /** One hundred thousand tonnes: above what any plot can deliver in a campaign, so more is a typing error. */
    public static final double MAX_KILOGRAMS = 100_000_000.0;

    public OliveWeight {
        if (kilograms == null || !Double.isFinite(kilograms) || kilograms < 0.0) {
            throw new IllegalArgumentException("settlement.weight.invalid");
        }
        if (kilograms > MAX_KILOGRAMS) {
            throw new IllegalArgumentException("settlement.weight.too_large");
        }
    }

    public OliveWeight plus(OliveWeight other) {
        return new OliveWeight(kilograms + other.kilograms);
    }
}
