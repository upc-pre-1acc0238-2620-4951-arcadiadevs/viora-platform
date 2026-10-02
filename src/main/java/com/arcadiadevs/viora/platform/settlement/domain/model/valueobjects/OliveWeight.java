package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

/**
 * Weight of delivered olives in kilograms; finite and not negative.
 *
 * @param kilograms weight in kilograms
 */
public record OliveWeight(Double kilograms) {

    public OliveWeight {
        if (kilograms == null || !Double.isFinite(kilograms) || kilograms < 0.0) {
            throw new IllegalArgumentException("settlement.weight.invalid");
        }
    }

    public OliveWeight plus(OliveWeight other) {
        return new OliveWeight(kilograms + other.kilograms);
    }
}
